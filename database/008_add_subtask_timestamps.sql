BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE public.subtasks
    ADD COLUMN starts_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN ends_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT subtasks_period_check
        CHECK (
            (starts_at IS NULL AND ends_at IS NULL)
            OR
            (
                starts_at IS NOT NULL
                AND ends_at IS NOT NULL
                AND ends_at > starts_at
            )
        );

COMMIT;
