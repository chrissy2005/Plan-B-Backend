BEGIN;

CREATE TABLE projekte (
    projekt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    beschreibung TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'AKTIV',

    CONSTRAINT projekte_status_check
        CHECK (status IN ('AKTIV', 'ABGESCHLOSSEN'))
);

CREATE TABLE projektmitglieder (
    projekt_id BIGINT NOT NULL,
    benutzer_id BIGINT NOT NULL,
    rolle VARCHAR(20) NOT NULL DEFAULT 'TEAMMITGLIED',

    PRIMARY KEY (projekt_id, benutzer_id),

    CONSTRAINT projektmitglieder_projekt_fk
        FOREIGN KEY (projekt_id)
        REFERENCES projekte (projekt_id),

    CONSTRAINT projektmitglieder_benutzer_fk
        FOREIGN KEY (benutzer_id)
        REFERENCES benutzer (benutzer_id),

    CONSTRAINT projektmitglieder_rolle_check
        CHECK (rolle IN ('PROJEKTLEITER', 'TEAMMITGLIED'))
);

COMMIT;