package org.planb.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.planb.dto.CreateLongToDoRequest;
import org.planb.dto.TaskRequest;
import org.planb.model.TaskPriority;
import org.planb.model.User;
import org.planb.repository.ProjectMembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Adapter over the existing meeting/long-todo model, not a second task store.
 * Prefixes prevent collisions between independently generated numeric IDs.
 */
@Service
public class TaskService {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private final JdbcTemplate jdbc;
    private final ProjectService projects;
    private final ProjectMembershipRepository memberships;

    public TaskService(JdbcTemplate jdbc, ProjectService projects, ProjectMembershipRepository memberships) {
        this.jdbc = jdbc;
        this.projects = projects;
        this.memberships = memberships;
    }

    private record TaskKey(boolean meeting, long id) {
        String table() { return meeting ? "meetings" : "long_todos"; }
        String column() { return meeting ? "meeting_id" : "todo_id"; }
        String participants() { return meeting ? "meeting_participants" : "todo_participants"; }
        String externalId() { return (meeting ? "m" : "t") + id; }
    }

    private TaskKey key(String id) {
        if (id == null || !id.matches("[mt][1-9][0-9]*")) {
            throw new IllegalArgumentException("task ID must be m<meetingId> or t<longToDoId>");
        }
        try { return new TaskKey(id.charAt(0) == 'm', Long.parseLong(id.substring(1))); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("invalid task ID"); }
    }

    private Map<String, Object> row(Long projectId, TaskKey key) {
        // All dynamic identifiers originate exclusively from TaskKey constants.
        return jdbc.queryForList("SELECT * FROM " + key.table() + " WHERE project_id = ? AND "
                + key.column() + " = ?", projectId, key.id()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
    }

    private String date(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().atZone(ZONE).toLocalDateTime().toString();
        if (value instanceof OffsetDateTime offset) return offset.atZoneSameInstant(ZONE).toLocalDateTime().toString();
        return Objects.toString(value, null);
    }

    private Map<String, Object> response(Long projectId, TaskKey key) {
        Map<String, Object> row = row(projectId, key);
        Map<String, Object> task = new HashMap<>();
        task.put("id", key.externalId());
        task.put("projectId", projectId);
        task.put("type", key.meeting() ? "MEETING" : "LANGES_TODO");
        task.put("title", row.get("title"));
        task.put("description", row.get("description"));
        task.put("start", date(row.get("starts_at")));
        task.put("end", date(row.get("ends_at")));
        task.put("priority", row.get("priority"));
        task.put("done", row.get("is_completed"));
        task.put("allParticipants", false); // "all" is expanded into a snapshot of current members.
        task.put("participantIds", jdbc.queryForList("SELECT user_id FROM " + key.participants()
                + " WHERE " + key.column() + " = ? ORDER BY user_id", Long.class, key.id()));
        task.put("doneBy", null);
        task.put("doneAt", null); // Existing schema has no completion audit columns.
        List<Map<String, Object>> subtasks = key.meeting() ? List.of() : jdbc.queryForList("""
                SELECT subtask_id AS id, title, description, assignee_id AS "assigneeId",
                is_completed AS done FROM subtasks WHERE todo_id = ? ORDER BY subtask_id
                """, key.id());
        task.put("subtasks", subtasks);
        return task;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long projectId, String email) {
        projects.requireProjectMember(projectId, email);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : jdbc.queryForList("SELECT meeting_id FROM meetings WHERE project_id = ? ORDER BY meeting_id", Long.class, projectId))
            result.add(response(projectId, new TaskKey(true, id)));
        for (Long id : jdbc.queryForList("SELECT todo_id FROM long_todos WHERE project_id = ? ORDER BY todo_id", Long.class, projectId))
            result.add(response(projectId, new TaskKey(false, id)));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long projectId, String taskId, String email) {
        projects.requireProjectMember(projectId, email);
        return response(projectId, key(taskId));
    }

    private TaskPriority priority(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "HOCH", "HIGH" -> TaskPriority.HIGH;
            case "MITTEL", "MEDIUM" -> TaskPriority.MEDIUM;
            case "NIEDRIG", "LOW" -> TaskPriority.LOW;
            default -> throw new IllegalArgumentException("invalid priority");
        };
    }

    private boolean meeting(TaskRequest input) {
        return switch (input.type()) {
            case "MEETING" -> true;
            case "LANGES_TODO", "LONG_TODO" -> false;
            default -> throw new IllegalArgumentException("invalid task type");
        };
    }

    private List<Long> validate(Long projectId, TaskRequest input) {
        boolean meeting = meeting(input);
        if ((meeting && input.start() == null) || (input.start() != null && !input.end().isAfter(input.start())))
            throw new IllegalArgumentException("end must be after start");
        priority(input.priority());
        List<Long> ids = input.allParticipants()
                ? memberships.findAllByProjectId(projectId).stream().map(m -> m.getUserId()).toList()
                : input.participantIds() == null ? List.of() : input.participantIds();
        if (ids.isEmpty() || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("participants required and must be unique");
        for (Long id : ids) {
            if (!memberships.existsByProjectIdAndUserId(projectId, id))
                throw new IllegalArgumentException("participant is not a project member");
        }
        if (meeting && input.subtasks() != null && !input.subtasks().isEmpty())
            throw new IllegalArgumentException("meetings cannot have subtasks");
        HashSet<Long> subtaskIds = new HashSet<>();
        for (TaskRequest.SubtaskInput sub : input.subtasks() == null ? List.<TaskRequest.SubtaskInput>of() : input.subtasks()) {
            if (sub.id() != null && !subtaskIds.add(sub.id()))
                throw new IllegalArgumentException("duplicate subtask ID");
            if (sub.assigneeId() != null && !ids.contains(sub.assigneeId()))
                throw new IllegalArgumentException("subtask assignee must be a participant");
        }
        return ids;
    }

    private OffsetDateTime timestamp(LocalDateTime value) { return value.atZone(ZONE).toOffsetDateTime(); }
    // The legacy schema requires a start. An omitted long-todo start becomes Berlin midnight of its deadline.
    private LocalDateTime start(TaskRequest input) {
        return input.start() != null ? input.start() : input.end().toLocalDate().atStartOfDay();
    }

    @Transactional
    public Map<String, Object> create(Long projectId, TaskRequest input, String email) {
        projects.requireProjectLeader(projectId, email);
        List<Long> ids = validate(projectId, input);
        if (!input.end().isAfter(start(input))) throw new IllegalArgumentException("end must be after start");
        TaskKey key;
        if (meeting(input)) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO meetings (project_id, title, description, starts_at, ends_at, priority)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING meeting_id
                    """, Long.class, projectId, input.title().trim(), input.description(),
                    timestamp(start(input)), timestamp(input.end()), priority(input.priority()).name());
            key = new TaskKey(true, id);
        } else {
            // Preserve and reuse the existing transactional creation logic.
            CreateLongToDoRequest legacy = new CreateLongToDoRequest();
            legacy.setTitle(input.title().trim());
            legacy.setDescription(input.description());
            legacy.setStartDateTime(start(input));
            legacy.setEndDateTime(input.end());
            legacy.setPriority(priority(input.priority()));
            legacy.setAssignedMemberIds(ids);
            legacy.setSubtasks(List.of());
            key = new TaskKey(false, projects.createLongToDo(projectId, legacy, email).getLongToDoId());
            // Use explicit Berlin offsets for this adapter, independently of the JVM timezone.
            jdbc.update("UPDATE long_todos SET starts_at = ?, ends_at = ? WHERE todo_id = ?",
                    timestamp(start(input)), timestamp(input.end()), key.id());
        }
        replaceParticipants(key, ids);
        if (!key.meeting()) saveSubtasks(key, input.subtasks());
        return response(projectId, key);
    }

    private void replaceParticipants(TaskKey key, List<Long> ids) {
        if (!key.meeting()) jdbc.update("UPDATE subtasks SET assignee_id = NULL WHERE todo_id = ?", key.id());
        jdbc.update("DELETE FROM " + key.participants() + " WHERE " + key.column() + " = ?", key.id());
        for (Long id : ids) jdbc.update("INSERT INTO " + key.participants() + " (" + key.column()
                + ", user_id) VALUES (?, ?)", key.id(), id);
    }

    private void saveSubtasks(TaskKey key, List<TaskRequest.SubtaskInput> input) {
        List<TaskRequest.SubtaskInput> list = input == null ? List.of() : input;
        List<Long> existing = jdbc.queryForList("SELECT subtask_id FROM subtasks WHERE todo_id = ?", Long.class, key.id());
        for (TaskRequest.SubtaskInput sub : list) {
            if (sub.id() != null && !existing.contains(sub.id()))
                throw new IllegalArgumentException("subtask does not belong to task");
        }
        for (Long id : existing) {
            if (list.stream().noneMatch(s -> id.equals(s.id()))) jdbc.update("DELETE FROM subtasks WHERE subtask_id = ?", id);
        }
        for (TaskRequest.SubtaskInput sub : list) {
            if (sub.id() == null) {
                jdbc.update("INSERT INTO subtasks (todo_id, title, description, assignee_id, is_completed, priority) VALUES (?, ?, ?, ?, ?, ?)",
                        key.id(), sub.title().trim(), sub.description(), sub.assigneeId(), sub.done(), "MEDIUM");
            } else {
                jdbc.update("UPDATE subtasks SET title = ?, description = ?, assignee_id = ?, is_completed = ? WHERE todo_id = ? AND subtask_id = ?",
                        sub.title().trim(), sub.description(), sub.assigneeId(), sub.done(), key.id(), sub.id());
            }
        }
    }

    @Transactional
    public Map<String, Object> update(Long projectId, String taskId, TaskRequest input, String email) {
        projects.requireProjectLeader(projectId, email);
        TaskKey key = key(taskId);
        row(projectId, key);
        if (key.meeting() != meeting(input)) throw new IllegalArgumentException("task type cannot change");
        List<Long> ids = validate(projectId, input);
        if (!input.end().isAfter(start(input))) throw new IllegalArgumentException("end must be after start");
        jdbc.update("UPDATE " + key.table() + " SET title = ?, description = ?, starts_at = ?, ends_at = ?, priority = ? WHERE "
                + key.column() + " = ?", input.title().trim(), input.description(), timestamp(start(input)),
                timestamp(input.end()), priority(input.priority()).name(), key.id());
        replaceParticipants(key, ids);
        if (!key.meeting()) saveSubtasks(key, input.subtasks());
        return response(projectId, key);
    }

    @Transactional
    public void delete(Long projectId, String taskId, String email) {
        projects.requireProjectLeader(projectId, email);
        TaskKey key = key(taskId);
        row(projectId, key);
        if (!key.meeting()) jdbc.update("DELETE FROM subtasks WHERE todo_id = ?", key.id());
        jdbc.update("DELETE FROM " + key.table() + " WHERE " + key.column() + " = ?", key.id());
    }

    @Transactional
    public Map<String, Object> assignSelf(Long projectId, String taskId, String email, boolean assign) {
        // Share the project lock with leader mutations to avoid membership/assignment races.
        lockProject(projectId);
        User user = projects.requireProjectMember(projectId, email);
        TaskKey key = key(taskId);
        row(projectId, key);
        if (assign) {
            jdbc.update("INSERT INTO " + key.participants() + " (" + key.column()
                    + ", user_id) VALUES (?, ?) ON CONFLICT DO NOTHING", key.id(), user.getId());
        } else {
            if (!key.meeting()) jdbc.update("UPDATE subtasks SET assignee_id = NULL WHERE todo_id = ? AND assignee_id = ?", key.id(), user.getId());
            jdbc.update("DELETE FROM " + key.participants() + " WHERE " + key.column() + " = ? AND user_id = ?", key.id(), user.getId());
        }
        return response(projectId, key);
    }

    private void lockProject(Long projectId) {
        if (jdbc.queryForList("SELECT project_id FROM projects WHERE project_id = ? FOR UPDATE",
                Long.class, projectId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "project not found");
    }

    @Transactional
    public Map<String, Object> status(Long projectId, String taskId, String status, String email) {
        lockProject(projectId);
        User user = projects.requireProjectMember(projectId, email);
        TaskKey key = key(taskId);
        row(projectId, key);
        if (!List.of("done", "open").contains(status)) throw new IllegalArgumentException("status must be done or open");
        boolean participant = !jdbc.queryForList("SELECT user_id FROM " + key.participants()
                + " WHERE " + key.column() + " = ? AND user_id = ?", Long.class, key.id(), user.getId()).isEmpty();
        boolean leader = memberships.findByProjectIdAndUserId(projectId, user.getId()).orElseThrow().getRole()
                == org.planb.model.ProjectRole.PROJECT_LEADER;
        if (!leader && !participant) throw new SecurityException("no permission");
        jdbc.update("UPDATE " + key.table() + " SET is_completed = ? WHERE " + key.column() + " = ?", status.equals("done"), key.id());
        return response(projectId, key);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calendar(String email) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var project : projects.getProjectView(email)) {
            Long projectId = project.getProjectId();
            Long userId = projects.requireProjectMember(projectId, email).getId();
            for (Map<String, Object> task : list(projectId, email)) {
                if (((List<?>) task.get("participantIds")).contains(userId)) result.add(task);
            }
        }
        return result;
    }

    private Long subtaskProject(TaskKey key) {
        if (key.meeting()) throw new IllegalArgumentException("meetings cannot have subtasks");
        return jdbc.queryForList("SELECT project_id FROM long_todos WHERE todo_id = ?", Long.class, key.id())
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
    }

    @Transactional
    public Map<String, Object> claimSubtask(String taskId, Long subtaskId, String email) {
        TaskKey key = key(taskId);
        Long projectId = subtaskProject(key);
        lockProject(projectId);
        User user = projects.requireProjectMember(projectId, email);
        Map<String, Object> sub = jdbc.queryForList("SELECT assignee_id FROM subtasks WHERE todo_id = ? AND subtask_id = ?",
                key.id(), subtaskId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "subtask not found"));
        if (sub.get("assignee_id") != null && !Objects.equals(((Number) sub.get("assignee_id")).longValue(), user.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "subtask already assigned");
        jdbc.update("INSERT INTO todo_participants (todo_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING", key.id(), user.getId());
        jdbc.update("UPDATE subtasks SET assignee_id = ? WHERE todo_id = ? AND subtask_id = ?", user.getId(), key.id(), subtaskId);
        return response(projectId, key);
    }

    @Transactional
    public Map<String, Object> subtaskStatus(String taskId, Long subtaskId, boolean done, String email) {
        TaskKey key = key(taskId);
        Long projectId = subtaskProject(key);
        lockProject(projectId);
        User user = projects.requireProjectMember(projectId, email);
        Map<String, Object> sub = jdbc.queryForList("SELECT assignee_id FROM subtasks WHERE todo_id = ? AND subtask_id = ?",
                key.id(), subtaskId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "subtask not found"));
        boolean leader = memberships.findByProjectIdAndUserId(projectId, user.getId()).orElseThrow().getRole()
                == org.planb.model.ProjectRole.PROJECT_LEADER;
        boolean assigned = sub.get("assignee_id") != null
                && Objects.equals(((Number) sub.get("assignee_id")).longValue(), user.getId());
        if (!leader && !assigned) throw new SecurityException("no permission");
        jdbc.update("UPDATE subtasks SET is_completed = ? WHERE todo_id = ? AND subtask_id = ?", done, key.id(), subtaskId);
        jdbc.update("""
                UPDATE long_todos SET is_completed =
                    EXISTS (SELECT 1 FROM subtasks WHERE todo_id = ?)
                    AND NOT EXISTS (SELECT 1 FROM subtasks WHERE todo_id = ? AND NOT is_completed)
                WHERE todo_id = ?
                """, key.id(), key.id(), key.id());
        return response(projectId, key);
    }
}
