-- Additive migration. Run before deploying the unified tasks API.
-- No rows, existing columns or constraints are removed or rewritten.
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
COMMIT;
