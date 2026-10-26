# Plan B: GET-/POST-Vertrag vom 19.10.2026

Die vorhandenen Projekt- und Member-Prüfungen bleiben erhalten. Neue Meeting-
und Detail-/Untertask-Endpunkte ergänzen die getrennten Ressourcen; es gibt
keine allgemeine Task-API und keine Invitation-Endpunkte.

## Authentifizierung

- Öffentlich: POST `/users`, POST `/auth`.
- Geschützt: alle übrigen Routen. Token fehlt/ungültig/abgelaufen: 401
  `{"message":"token wrong"}`.
- Header: `Authorization: Bearer <JWT>`, separat `Content-Type: application/json`.
- Login mit falschen Zugangsdaten: 400 `{"message":"email or password wrong"}`.
- Rollen sind projektbezogen: MEMBER / PROJECT_LEADER.
- Echte Berechtigungsverweigerung als Projektmitglied: 403, `"no permission"`.
- Ressource fehlt oder Zugriff als Nichtmitglied: 404.
- JSON-/Enum-/Query-Fehler: 400; interne Datenbankfehler: 500 ohne SQL im Body.

## Implementierte GET-/POST-Routen

| Route | Vertrag |
|---|---|
| POST /users | firstName, lastName, email, password → 200, message |
| POST /auth | email, password → 200, message, token, userId |
| GET /users/me | id, firstName, lastName, email, name |
| GET /projects | Array aus projectId, name, status, role |
| POST /projects | name, description → 200, message, projectId; id bleibt als kompatibler Alias |
| GET /projects/{id} | Projekt mit Rolle und Erstellernamen |
| GET /projects/{id}/members | Array aus userId, firstName, lastName, email, role |
| POST /projects/{id}/members | email, role → 200, member added |
| GET /projects/{id}/meetings | Meeting-Array; from, to, sort unterstützt |
| POST /projects/{id}/meetings | title, description, startDateTime, endDateTime, participantIds → 200, message, meetingId |
| GET /meetings/{id} | Meetingdetails mit Teilnehmern; start/end bleiben zusätzliche Detail-Aliasse |
| GET /projects/{id}/long-todos | Array; priority, done, sort unterstützt |
| POST /projects/{id}/long-todos | title, description, startDateTime, endDateTime, priority, assignedMemberIds, subtasks → 200, message, longToDoId |
| GET /long-todos/{id} | Details mit Teilnehmern und Untertasks, inkl. Assignee-Namen |
| POST /long-todos/{id}/subtasks | title, description, optional priority → 200, message, subtaskId |

Listen bleiben wie im API-Design direkte Arrays. Detailobjekte bleiben mit dem
bisherigen Frontend kompatibel; die allgemeine Aussage „jede Antwort mit message“
steht in der Vorlage im Widerspruch zu diesen Array-/Detailbeispielen.

## Validierungen und offene Entscheidungen

- Zeitangaben: ISO-LocalDateTime, z. B. `2026-11-01T10:00`, ohne Offset.
  Bestehende TIMESTAMPTZ-Spalten werden nicht migriert. Eine verbindliche
  fachliche Zeitzonenregel für den späteren externen Kalender ist noch offen.
- Titel und beide Zeitpunkte sind erforderlich; Ende muss nach Start liegen.
- Mindestens ein Teilnehmer, keine Duplikate, nur Mitglieder dieses Projekts.
- Nur PROJECT_LEADER erstellt Meetings, Long-To-dos oder Untertasks.
- Untertask-Priorität erhält bei fehlendem Wert MEDIUM. Neue Untertasks bleiben
  unzugewiesen; dieser Request ist kein US2-Claim.
- Member-Aufnahme serialisiert am Projekt-Datensatz: zwei parallele POSTs auf
  dieselbe E-Mail ändern nicht unbemerkt deren Rolle.
- Meeting-Priorität bleibt die vorhandene feste MEDIUM-Antwort. Keine neue
  Spalte und keine schemaändernde Migration.
- Sortierungen: startDateTime, endDateTime, title, priority; unbekannte Werte
  werden mit 400 abgelehnt. Prioritätssortierung: HIGH, MEDIUM, LOW.
- Datumsfilter bei Meetings: Intervalle überlappen den inklusiven Kalendertag-
  Bereich from/to; Listen ohne Filter bleiben nutzbar.

## Nicht Bestandteil dieser Korrektur

PUT/DELETE für Projekte, Member und Aufgaben, Schutz der letzten Leitung bei
diesen noch fehlenden Operationen, DELETE /auth mit tatsächlicher Revokation,
PUT /users, Kalender, Verfügbarkeit und US2-Übernahme sind weiter offen. Fehlende
Routen werden nicht länger irreführend als 403 maskiert.

## Build und Regressionstests

Java 25 und eine isolierte PostgreSQL-Datenbank mit den vorhandenen Migrationen
001–007 bereitstellen. Keine Tests gegen eine produktive Datenbank ausführen.

```sh
export JAVA_HOME=/path/to/jdk25
export PATH="$JAVA_HOME/bin:$PATH"
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/planb_test
export SPRING_DATASOURCE_USERNAME=planb_test
export SPRING_DATASOURCE_PASSWORD='<lokales Testpasswort>'
bash ./gradlew clean build --console=plain
```

Die Integrationstests erzeugen eigene eindeutig benannte Testbenutzer und
Projekte in der angegebenen Datenbank. Sie prüfen JWT-Fehler, Member-Responses,
Fehlercodes, parallele Member-Aufnahme, Rollenrechte, Erstellung/Persistenz,
Details, Filter und ungültige Eingaben. Die Testdaten bleiben zum Nachprüfen in
der lokalen Testdatenbank; keine Produktionstests und keine Schema-Migration
werden automatisch gegen Heroku ausgeführt.
