BEGIN;

CREATE TABLE lange_todos (
    todo_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    projekt_id BIGINT NOT NULL,
    titel VARCHAR(150) NOT NULL,
    beschreibung TEXT,
    startdatum DATE NOT NULL,
    enddatum DATE NOT NULL,
    erledigt BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT lange_todos_projekt_fk
        FOREIGN KEY (projekt_id)
        REFERENCES projekte (projekt_id),

    CONSTRAINT lange_todos_zeitraum_check
        CHECK (enddatum >= startdatum)
);

CREATE TABLE todo_beteiligte (
    todo_id BIGINT NOT NULL,
    benutzer_id BIGINT NOT NULL,

    PRIMARY KEY (todo_id, benutzer_id),

    CONSTRAINT todo_beteiligte_todo_fk
        FOREIGN KEY (todo_id)
        REFERENCES lange_todos (todo_id)
        ON DELETE CASCADE,

    CONSTRAINT todo_beteiligte_benutzer_fk
        FOREIGN KEY (benutzer_id)
        REFERENCES benutzer (benutzer_id)
);

CREATE TABLE teilaufgaben (
    teilaufgabe_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    todo_id BIGINT NOT NULL,
    titel VARCHAR(150) NOT NULL,
    beschreibung TEXT,
    erledigt BOOLEAN NOT NULL DEFAULT FALSE,
    bearbeiter_id BIGINT,

    CONSTRAINT teilaufgaben_todo_fk
        FOREIGN KEY (todo_id)
        REFERENCES lange_todos (todo_id)
        ON DELETE CASCADE,

    CONSTRAINT teilaufgaben_bearbeiter_fk
        FOREIGN KEY (todo_id, bearbeiter_id)
        REFERENCES todo_beteiligte (todo_id, benutzer_id)
);

COMMIT;