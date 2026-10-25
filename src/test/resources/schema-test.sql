-- Isolated test schema matching production migrations 001-008.
-- No DROP/TRUNCATE: a wrongly selected database is never reset.
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS projects (
    project_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL, description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED')),
    created_user_id BIGINT REFERENCES users(user_id)
);
CREATE TABLE IF NOT EXISTS projectmembership (
    project_id BIGINT NOT NULL REFERENCES projects(project_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('PROJECT_LEADER', 'MEMBER')),
    PRIMARY KEY (project_id, user_id)
);
CREATE TABLE IF NOT EXISTS meetings (
    meeting_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(project_id), title VARCHAR(150) NOT NULL,
    description TEXT, starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL, is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CHECK (ends_at > starts_at)
);
CREATE TABLE IF NOT EXISTS meeting_participants (
    meeting_id BIGINT NOT NULL REFERENCES meetings(meeting_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id), PRIMARY KEY (meeting_id, user_id)
);
CREATE TABLE IF NOT EXISTS long_todos (
    todo_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(project_id), title VARCHAR(150) NOT NULL,
    description TEXT, starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL, is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) NOT NULL DEFAULT 'LOW' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CHECK (ends_at >= starts_at)
);
CREATE TABLE IF NOT EXISTS todo_participants (
    todo_id BIGINT NOT NULL REFERENCES long_todos(todo_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id), PRIMARY KEY (todo_id, user_id)
);
CREATE TABLE IF NOT EXISTS subtasks (
    subtask_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    todo_id BIGINT NOT NULL REFERENCES long_todos(todo_id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL, description TEXT, is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    assignee_id BIGINT, priority VARCHAR(20) NOT NULL DEFAULT 'LOW' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    FOREIGN KEY (todo_id, assignee_id) REFERENCES todo_participants(todo_id, user_id)
);
