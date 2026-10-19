\set ON_ERROR_STOP on

BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

-- Verhindert gleichzeitige Änderungen während der Migration.
LOCK TABLE public.long_todos, public.subtasks
    IN ACCESS EXCLUSIVE MODE;

-- Bestehende Datumswerte nicht ohne Absprache umwandeln.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.long_todos) THEN
        RAISE EXCEPTION
            'Es existieren lange To-dos. Vor der Migration die Uhrzeiten fuer bestehende Aufgaben festlegen.';
    END IF;
END;
$$;

-- Priorität für lange To-dos.
ALTER TABLE public.long_todos
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'LOW',
    ADD CONSTRAINT long_todos_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

-- Priorität für Teilaufgaben.
ALTER TABLE public.subtasks
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'LOW',
    ADD CONSTRAINT subtasks_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

-- Datumsspalten auf Datum und Uhrzeit umstellen.
ALTER TABLE public.long_todos
    ALTER COLUMN start_date TYPE TIMESTAMP WITH TIME ZONE
        USING (start_date::timestamp AT TIME ZONE 'Europe/Berlin'),
    ALTER COLUMN end_date TYPE TIMESTAMP WITH TIME ZONE
        USING (end_date::timestamp AT TIME ZONE 'Europe/Berlin');

-- Einheitliche Benennung wie bei Meetings.
ALTER TABLE public.long_todos
    RENAME COLUMN start_date TO starts_at;

ALTER TABLE public.long_todos
    RENAME COLUMN end_date TO ends_at;

COMMIT;