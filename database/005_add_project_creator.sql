BEGIN;

ALTER TABLE projekte
    ADD COLUMN created_by_user_id BIGINT NOT NULL,
    ADD CONSTRAINT projekte_created_by_user_fk
        FOREIGN KEY (created_by_user_id)
        REFERENCES benutzer (benutzer_id);

COMMIT;