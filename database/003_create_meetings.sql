BEGIN;

CREATE TABLE meetings (
    meeting_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    projekt_id BIGINT NOT NULL,
    titel VARCHAR(150) NOT NULL,
    beschreibung TEXT,
    beginn TIMESTAMP WITH TIME ZONE NOT NULL,
    ende TIMESTAMP WITH TIME ZONE NOT NULL,
    erledigt BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT meetings_projekt_fk
        FOREIGN KEY (projekt_id)
        REFERENCES projekte (projekt_id),

    CONSTRAINT meetings_zeitraum_check
        CHECK (ende > beginn)
);

CREATE TABLE meetingteilnehmer (
    meeting_id BIGINT NOT NULL,
    benutzer_id BIGINT NOT NULL,

    PRIMARY KEY (meeting_id, benutzer_id),

    CONSTRAINT meetingteilnehmer_meeting_fk
        FOREIGN KEY (meeting_id)
        REFERENCES meetings (meeting_id)
        ON DELETE CASCADE,

    CONSTRAINT meetingteilnehmer_benutzer_fk
        FOREIGN KEY (benutzer_id)
        REFERENCES benutzer (benutzer_id)
);

COMMIT;