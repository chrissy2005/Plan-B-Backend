-- Apply once, after migrations 001-005, using psql.
-- Coordinate with the team: stop backends using the old schema before applying.
-- Deploy the matching JdbcUserRepository before restarting the backend.
-- On any error in an interactive psql session, run ROLLBACK; before retrying.
-- Existing data, IDs, foreign keys and delete rules are preserved.
-- Only project status and membership role values are translated.
-- Internal identity sequence names are unchanged; generated IDs continue normally.
\set ON_ERROR_STOP on

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

-- Rename existing tables (do not recreate them).
ALTER TABLE public.benutzer RENAME TO users;
ALTER TABLE public.projekte RENAME TO projects;
ALTER TABLE public.projektmitglieder RENAME TO projectmembership;
ALTER TABLE public.meetingteilnehmer RENAME TO meeting_participants;
ALTER TABLE public.lange_todos RENAME TO long_todos;
ALTER TABLE public.todo_beteiligte RENAME TO todo_participants;
ALTER TABLE public.teilaufgaben RENAME TO subtasks;

-- Rename columns; PostgreSQL retains the relationships between these objects.
ALTER TABLE public.users RENAME COLUMN benutzer_id TO user_id;
ALTER TABLE public.users RENAME COLUMN vorname TO first_name;
ALTER TABLE public.users RENAME COLUMN nachname TO last_name;
ALTER TABLE public.users RENAME COLUMN passwort_hash TO password_hash;

ALTER TABLE public.projects RENAME COLUMN projekt_id TO project_id;
ALTER TABLE public.projects RENAME COLUMN beschreibung TO description;
ALTER TABLE public.projects RENAME COLUMN created_by_user_id TO created_user_id;

ALTER TABLE public.projectmembership RENAME COLUMN projekt_id TO project_id;
ALTER TABLE public.projectmembership RENAME COLUMN benutzer_id TO user_id;
ALTER TABLE public.projectmembership RENAME COLUMN rolle TO role;

ALTER TABLE public.meetings RENAME COLUMN projekt_id TO project_id;
ALTER TABLE public.meetings RENAME COLUMN titel TO title;
ALTER TABLE public.meetings RENAME COLUMN beschreibung TO description;
ALTER TABLE public.meetings RENAME COLUMN beginn TO starts_at;
ALTER TABLE public.meetings RENAME COLUMN ende TO ends_at;
ALTER TABLE public.meetings RENAME COLUMN erledigt TO is_completed;

ALTER TABLE public.meeting_participants RENAME COLUMN benutzer_id TO user_id;

ALTER TABLE public.long_todos RENAME COLUMN projekt_id TO project_id;
ALTER TABLE public.long_todos RENAME COLUMN titel TO title;
ALTER TABLE public.long_todos RENAME COLUMN beschreibung TO description;
ALTER TABLE public.long_todos RENAME COLUMN startdatum TO start_date;
ALTER TABLE public.long_todos RENAME COLUMN enddatum TO end_date;
ALTER TABLE public.long_todos RENAME COLUMN erledigt TO is_completed;

ALTER TABLE public.todo_participants RENAME COLUMN benutzer_id TO user_id;

ALTER TABLE public.subtasks RENAME COLUMN teilaufgabe_id TO subtask_id;
ALTER TABLE public.subtasks RENAME COLUMN titel TO title;
ALTER TABLE public.subtasks RENAME COLUMN beschreibung TO description;
ALTER TABLE public.subtasks RENAME COLUMN erledigt TO is_completed;
ALTER TABLE public.subtasks RENAME COLUMN bearbeiter_id TO assignee_id;

-- Translate stored enum values and defaults, then restore the allowed-value checks.
ALTER TABLE public.projects DROP CONSTRAINT projekte_status_check;
ALTER TABLE public.projects ALTER COLUMN status SET DEFAULT 'ACTIVE';
UPDATE public.projects
SET status = CASE status
    WHEN 'AKTIV' THEN 'ACTIVE'
    WHEN 'ABGESCHLOSSEN' THEN 'COMPLETED'
    ELSE status
END;
ALTER TABLE public.projects ADD CONSTRAINT projects_status_check
    CHECK (status IN ('ACTIVE', 'COMPLETED'));

ALTER TABLE public.projectmembership DROP CONSTRAINT projektmitglieder_rolle_check;
ALTER TABLE public.projectmembership ALTER COLUMN role SET DEFAULT 'MEMBER';
UPDATE public.projectmembership
SET role = CASE role
    WHEN 'TEAMMITGLIED' THEN 'MEMBER'
    WHEN 'PROJEKTLEITER' THEN 'PROJECT_LEADER'
    ELSE role
END;
ALTER TABLE public.projectmembership ADD CONSTRAINT projectmembership_role_check
    CHECK (role IN ('PROJECT_LEADER', 'MEMBER'));

-- Rename existing constraints without changing their rules.
ALTER TABLE public.users RENAME CONSTRAINT benutzer_pkey TO users_pkey;
ALTER TABLE public.users RENAME CONSTRAINT benutzer_email_key TO users_email_key;
ALTER TABLE public.projects RENAME CONSTRAINT projekte_pkey TO projects_pkey;
ALTER TABLE public.projects RENAME CONSTRAINT projekte_created_by_user_fk TO projects_created_user_fk;
ALTER TABLE public.projectmembership RENAME CONSTRAINT projektmitglieder_pkey TO projectmembership_pkey;
ALTER TABLE public.projectmembership RENAME CONSTRAINT projektmitglieder_projekt_fk TO projectmembership_project_fk;
ALTER TABLE public.projectmembership RENAME CONSTRAINT projektmitglieder_benutzer_fk TO projectmembership_user_fk;
ALTER TABLE public.meetings RENAME CONSTRAINT meetings_projekt_fk TO meetings_project_fk;
ALTER TABLE public.meetings RENAME CONSTRAINT meetings_zeitraum_check TO meetings_period_check;
ALTER TABLE public.meeting_participants RENAME CONSTRAINT meetingteilnehmer_pkey TO meeting_participants_pkey;
ALTER TABLE public.meeting_participants RENAME CONSTRAINT meetingteilnehmer_meeting_fk TO meeting_participants_meeting_fk;
ALTER TABLE public.meeting_participants RENAME CONSTRAINT meetingteilnehmer_benutzer_fk TO meeting_participants_user_fk;
ALTER TABLE public.long_todos RENAME CONSTRAINT lange_todos_pkey TO long_todos_pkey;
ALTER TABLE public.long_todos RENAME CONSTRAINT lange_todos_projekt_fk TO long_todos_project_fk;
ALTER TABLE public.long_todos RENAME CONSTRAINT lange_todos_zeitraum_check TO long_todos_period_check;
ALTER TABLE public.todo_participants RENAME CONSTRAINT todo_beteiligte_pkey TO todo_participants_pkey;
ALTER TABLE public.todo_participants RENAME CONSTRAINT todo_beteiligte_todo_fk TO todo_participants_todo_fk;
ALTER TABLE public.todo_participants RENAME CONSTRAINT todo_beteiligte_benutzer_fk TO todo_participants_user_fk;
ALTER TABLE public.subtasks RENAME CONSTRAINT teilaufgaben_pkey TO subtasks_pkey;
ALTER TABLE public.subtasks RENAME CONSTRAINT teilaufgaben_todo_fk TO subtasks_todo_fk;
ALTER TABLE public.subtasks RENAME CONSTRAINT teilaufgaben_bearbeiter_fk TO subtasks_assignee_fk;

COMMIT;
