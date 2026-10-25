# Plan B API-Vertrag

Stand: 25. Oktober 2026. Vertrag der Änderungen im Audit-Branch, nicht automatisch der produktiven Heroku-Version. Das bestehende Members-Modell wird verwendet; es gibt keine Invitation-Routen.

## Authentifizierung und Fehler

Geschützte Requests senden `Authorization: Bearer <JWT>`. JSON-Requests senden zusätzlich `Content-Type: application/json`. Der Token gehört niemals in den JSON-Body. `/users` und `/auth` sind für POST öffentlich; alle übrigen hier dokumentierten Operationen benötigen ein JWT.

| Status | Bedeutung |
| --- | --- |
| 400 | Ungültiger Body, ungültiger Rollen-/Statuswert oder Validierungsfehler |
| 401 | Fehlender, ungültiger oder abgelaufener JWT; fehlgeschlagener Login |
| 403 | Authentifizierter Benutzer hat nicht die erforderliche Projekt-/Aufgabenberechtigung |
| 404 | Projekt, registrierter Zielbenutzer, Mitglied, Task oder Teilaufgabe nicht gefunden |
| 409 | Mitglied bereits vorhanden, letzte Projektleitung oder konkurrierender Zuweisungskonflikt |
| 500 | Unerwarteter interner Fehler; wird nicht mehr durch einen Security-Error-Dispatch zu 401/403 umgeschrieben |

Anwendungsfehler enthalten `{"message":"..."}`. Standardfehler von Spring MVC, z. B. unbekannte Routen oder fehlerhaftes JSON, können weiterhin das Spring-Fehlerformat besitzen. Das Frontend unterstützt beide Formen. Nicht erlaubte CORS-Origins werden vom CORS-Filter separat abgelehnt; das ist keine Projektberechtigungsprüfung.

## Benutzer und Sitzung

| Methode und Pfad | Request | Erfolg |
| --- | --- | --- |
| `POST /users` | `{"firstName":"Ada","lastName":"Test","email":"ada@example.test","password":"Test-only-42!"}` | 200 `{"message":"User created successfully"}` |
| `POST /auth` | `{"email":"ada@example.test","password":"Test-only-42!"}` | 200 `{"token":"<JWT>","message":"Login successful","userId":1}` |
| `DELETE /auth` | Kein Body | 204, leer |
| `GET /users/me` | Kein Body | 200 `{"id":1,"firstName":"Ada","lastName":"Test","name":"Ada Test","email":"ada@example.test"}` |

Registrierung normalisiert E-Mail-Adressen durch Trimmen und Kleinschreibung. Vor-/Nachname: Pflicht, jeweils maximal 100 Zeichen; E-Mail: Pflicht, gültige Adresse, maximal 254 Zeichen; Passwort: mindestens 8 Zeichen, zusätzlich gelten die Grenzen des bestehenden BCrypt-Encoders. Doppelte Registrierung bleibt mit der bestehenden Backend-Antwort 400 `User already exists` kompatibel.

Logout ist zustandslos: Das Frontend verwirft JWT, lokale Benutzer-ID und Benutzer-Cache. Bereits ausgestellte JWTs werden nicht serverseitig widerrufen und bleiben bis zum Ablauf gültig. Diese Route darf nicht als Token-Revocation beworben werden; ein dauerhaftes Revocation-Modell wäre eine eigene Erweiterung.

## Projekte

| Methode und Pfad | Request | Erfolg |
| --- | --- | --- |
| `GET /projects` | Kein Body | 200 Array von Projektübersichten |
| `POST /projects` | `{"name":"Plan B","description":"Beschreibung"}` | 200 `{"id":67,"name":"Plan B","description":"Beschreibung","createdByUserId":1,"message":"project created"}` |
| `GET /projects/{projectId}` | Kein Body | 200 Projektdetails |
| `PUT /projects/{projectId}` | `{"name":"Neuer Name","description":"Text","status":"COMPLETED"}` | 200 Projektdetails |
| `DELETE /projects/{projectId}` | Kein Body | 204, leer |

Projektübersicht: `{"projectId":67,"name":"Plan B","status":"ACTIVE","role":"PROJECT_LEADER"}`. Projektdetails: `{"projectId":67,"name":"Plan B","description":"Beschreibung","status":"ACTIVE","role":"PROJECT_LEADER","createdByUserLastName":"Test","createdByUserFirstName":"Ada"}`. `role` ist die Rolle des anfragenden Benutzers. Die Übersicht enthält nur dessen Projekte und keine Beschreibung. Details enthalten bewusst keine eingebettete Mitgliederliste; das Frontend lädt `/members` separat. Mitgliederanzahl und Änderungsdatum werden nicht erfunden.

Projektstatus: `ACTIVE` oder `COMPLETED`. Projektname: Pflicht, maximal 150 Zeichen; Beschreibung beim Update maximal 2000 Zeichen. Ein Update erfordert `name`, `description` und `status` sind optional; fehlender Status behält den bisherigen Wert. Lesen: alle Projektmitglieder. Ändern/Löschen: nur Projektleitung. Beim Löschen werden abhängige Teilaufgaben, Aufgaben, Teilnehmer und Mitgliedschaften in definierter Reihenfolge entfernt.

## Mitglieder

| Methode und Pfad | Request | Erfolg |
| --- | --- | --- |
| `GET /projects/{projectId}/members` | Kein Body | 200 Array von Mitgliedern |
| `POST /projects/{projectId}/members` | `{"email":"member@example.test","role":"member"}` | 200 `{"message":"member added"}` |
| `PUT /projects/{projectId}/members/{userId}` | `{"role":"leader"}` | 200 `{"message":"role updated"}` |
| `DELETE /projects/{projectId}/members/{userId}` | Kein Body | 204, leer |

Mitglied: `{"userId":1,"firstName":"Ada","lastName":"Test","name":"Ada Test","email":"ada@example.test","role":"PROJECT_LEADER"}`. Ausgabe-Rollen heißen `PROJECT_LEADER` oder `MEMBER`; Eingabe-Rollen für POST und PUT sind `leader` oder `member` (Groß-/Kleinschreibung wird toleriert). Die vorhandenen Eingabe-Aliasse `project_leader`, `projektleiter`, `projektleitung` und `teammitglied` bleiben kompatibel. Die Rolle ist bei POST und PUT Pflicht; das Frontend sendet sie immer explizit.

Nur die Projektleitung darf Mitglieder hinzufügen, Rollen ändern oder entfernen. Die E-Mail muss bereits registriert sein. Doppelte Mitgliedschaft ergibt 409 und überschreibt keine bestehende Rolle. Die letzte Projektleitung darf weder entfernt noch zum Mitglied herabgestuft werden. Eine `SELECT ... FOR UPDATE`-Sperre auf dem Projekt serialisiert diese Mutationen innerhalb einer Transaktion und verhindert Parallelitätsfehler, auch über mehrere Backend-Instanzen hinweg.

Wird ein Mitglied entfernt, werden seine Aufgaben-Teilnahmen und Teilaufgaben-Zuweisungen dieses Projekts bereinigt; Aufgaben selbst bleiben bestehen.

## Einheitliche Aufgaben

Die Routen sind ein Adapter auf `meetings`, `long_todos`, deren Teilnehmer und `subtasks`. Bestehende Long-To-do-Endpunkte und die bestehende Erstellungslogik bleiben erhalten. Es wird keine zweite Aufgaben-Datenbank angelegt.

### IDs und Request

`taskId` ist eine opake Zeichenkette: `m12` für Meeting 12, `t12` für Long-To-do 12. Präfixe verhindern Kollisionen der unabhängigen numerischen Primärschlüssel. Clients müssen diese IDs aus der Response verwenden und dürfen nicht ausschließlich Zahlen erwarten.

POST und PUT verwenden denselben nicht eingepackten Body:

```json
{
  "type": "LANGES_TODO",
  "title": "Dokumentation",
  "description": "API dokumentieren",
  "start": "2026-11-01T10:00",
  "end": "2026-11-01T11:00",
  "priority": "HOCH",
  "allParticipants": false,
  "participantIds": [1, 2],
  "subtasks": [
    {
      "id": 8,
      "title": "Vertrag prüfen",
      "description": "Felder abgleichen",
      "assigneeId": 2,
      "done": false
    }
  ]
}
```

- `type`: `MEETING`, `LANGES_TODO` oder Eingabealias `LONG_TODO`; bei einem Update unveränderlich.
- `title`: Pflicht, maximal 150 Zeichen; `description`: optional, maximal 2000 Zeichen.
- `end`: Pflicht; `start`: für Meetings Pflicht, bei Long-To-dos optional.
- Datum/Uhrzeit: lokale ISO-8601-Werte ohne Offset, Zeitzone `Europe/Berlin`. Bei fehlendem Long-To-do-Start wird 00:00 Uhr des Fälligkeitstags gespeichert. Ende muss nach dem effektiven Start liegen.
- `priority`: Pflicht; deutsch `NIEDRIG`, `MITTEL`, `HOCH` oder API-Werte `LOW`, `MEDIUM`, `HIGH`.
- `allParticipants`: `true` expandiert die aktuellen Projektmitglieder in eine Teilnehmerliste zum Speicherzeitpunkt. Es ist keine dynamische zukünftige Mitgliedschaft.
- `participantIds`: eindeutige numerische Benutzer-IDs aktueller Projektmitglieder; mindestens eine Teilnahme erforderlich, sofern nicht `allParticipants:true`.
- `subtasks`: optional bzw. `[]`; bei Meetings ausschließlich leer. Teilaufgaben brauchen einen Titel; optionaler Assignee muss Task-Teilnehmer sein.
- Teilaufgaben ohne `id` werden angelegt. Beim Update bleiben übergebene vorhandene IDs erhalten; nicht mehr übergebene Teilaufgaben werden entfernt. Fremde und doppelte IDs ergeben 400. Ein neuer Task darf keine vorhandenen Teilaufgaben-IDs übernehmen.
- PUT ersetzt den gesamten Bearbeitungszustand einschließlich Teilnehmern und Teilaufgaben. Kein `{"update": ...}`-Wrapper.

### Response

```json
{
  "id": "t12",
  "projectId": 67,
  "type": "LANGES_TODO",
  "title": "Dokumentation",
  "description": "API dokumentieren",
  "start": "2026-11-01T10:00",
  "end": "2026-11-01T11:00",
  "priority": "HIGH",
  "allParticipants": false,
  "participantIds": [1, 2],
  "done": false,
  "doneBy": null,
  "doneAt": null,
  "subtasks": [
    {"id":8,"title":"Vertrag prüfen","description":"Felder abgleichen","assigneeId":2,"done":false}
  ]
}
```

`doneBy` und `doneAt` bleiben `null`, da die bestehende Datenbank keine Abschluss-Audit-Spalten hat. `allParticipants` wird als `false` ausgegeben, da explizite Teilnehmer gespeichert werden. Das Frontend normalisiert Rollen, Prioritäten und numerische Teilnehmer-/Assignee-IDs.

### Routen und Rechte

| Methode und Pfad | Request | Erfolg und Recht |
| --- | --- | --- |
| `GET /projects/{projectId}/tasks` | Kein Body | 200 Task-Array; Projektmitglied |
| `POST /projects/{projectId}/tasks` | Task-Body | 201 Task; Projektleitung |
| `GET /projects/{projectId}/tasks/{taskId}` | Kein Body | 200 Task; Projektmitglied |
| `PUT /projects/{projectId}/tasks/{taskId}` | Vollständiger Task-Body | 200 Task; Projektleitung |
| `DELETE /projects/{projectId}/tasks/{taskId}` | Kein Body | 204; Projektleitung |
| `PUT /projects/{projectId}/tasks/{taskId}/assignees/me` | Kein Body | 200 Task; Projektmitglied, idempotente Selbstzuweisung |
| `DELETE /projects/{projectId}/tasks/{taskId}/assignees/me` | Kein Body | 200 Task; Projektmitglied, entfernt nur eigene Teilnahme |
| `PUT /projects/{projectId}/tasks/{taskId}/status` | `{"status":"done"}` oder `{"status":"open"}` | 200 Task; Projektleitung oder Task-Teilnehmer |
| `GET /users/me/calendar` | Kein Body | 200 Task-Array der eigenen Teilnahmen in den eigenen Projekten |

Entfernen einer eigenen Long-To-do-Teilnahme löst auch eigene Teilaufgaben-Zuweisungen. Ein Task eines anderen Projekts wird unter der falschen Projekt-ID nicht ausgeliefert.

### Bereits vom Frontend benötigte Teilaufgabenaktionen

| Methode und Pfad | Request | Erfolg |
| --- | --- | --- |
| `POST /tasks/{taskId}/subtasks/{subtaskId}/claim` | Kein Body | 200 vollständiger Task |
| `PATCH /tasks/{taskId}/subtasks/{subtaskId}` | `{"done":true}` | 200 `{"task":{...}}` |

Übernahme ist nur für Projektmitglieder möglich und ergibt 409, falls eine andere Person bereits zugewiesen ist. Abschluss ist nur der zugewiesenen Person oder Projektleitung erlaubt. Der Task-Abschluss wird bei dieser Aktion aus allen Teilaufgaben neu berechnet.

## CORS

Die bestehende Produktions-Allowlist bleibt unverändert:

- `https://plan-b-frontend-1a40950601f6.herokuapp.com`

Erlaubte Methoden: GET, POST, PUT, PATCH, DELETE, OPTIONS. Erlaubte Header: Authorization, Content-Type, Accept. Preflight ist ohne JWT zulässig. Die lokale HTTP-Testsuite prüft den Heroku-Origin und die CORS-Header auch bei 401-Antworten.

## Noch nicht implementierte Zusatzfunktionen

Keiner der 22 im Auftrag genannten Endpunkte fehlt im Audit-Branch. Die bereits im Frontend angelegten Zusatzfunktionen für Verfügbarkeitsabfrage, freie Zeitfenster, Teilaufgaben-Zeitplanung und Benutzer-Zeitplan besitzen weiterhin keine Backend-Routen. Sie sind keine Invitation-Funktionen und wurden nicht durch erfundene Daten ersetzt. Einladungs-Kompatibilitätsfunktionen lösen keine Netzwerkrequests aus.

Es gibt weiterhin keine eigenständigen Benutzer-/Kalender-HTML-Seiten im Frontend. Tote Navigationseinträge wurden entfernt; `GET /users/me/calendar` ist als API vorhanden.

## Build und Tests

Benötigt: JDK 25 und Gradle Wrapper. `bash ./gradlew clean build` führt ohne externe Datenbank den H2-Kontexttest aus; die PostgreSQL-HTTP-Tests werden dann explizit übersprungen.

Die vollständige Suite braucht eine isolierte lokale PostgreSQL-Datenbank, deren Name mit `planb_test` beginnt. Niemals Produktionszugangsdaten verwenden.

```sh
export PLANB_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/planb_test
export PLANB_TEST_DATABASE_USER=planb_test
export PLANB_TEST_DATABASE_PASSWORD='<lokales-testpasswort>'
bash ./gradlew clean build
```

`schema-test.sql` legt ausschließlich fehlende Testtabellen an; es löscht keine vorhandenen Tabellen oder Daten. Die Tests erzeugen neue zufällige Benutzer und Projekte und verändern nur ihre eigenen Fixtures. Zusätzlich wurde die vollständige Suite gegen eine zweite isolierte Datenbank ausgeführt, die zuvor mit den originalen Migrationen `001` bis `008` aufgebaut wurde.

## Deployment-Voraussetzung

Vor dem Backend-Deploy muss ausschließlich die neue additive Migration `database/008_add_meeting_priority.sql` auf einer bereits bis `007` migrierten Produktionsdatenbank ausgeführt werden. Erst Schema-Stand und Backup prüfen, danach die Migration separat freigeben. Sie ergänzt nur `meetings.priority`, ist wiederholbar und hat ein fünfsekündiges Lock-Timeout.

Die vorhandene Migration `007` befüllt Zeitspalten und sollte nicht ungeprüft erneut auf Produktion ausgeführt werden. Diese Arbeit hat keine produktiven SQL-Befehle ausgeführt und keinen Heroku-Deploy ausgelöst.
