package org.planb.service;

import org.planb.controller.ApiException;
import org.planb.dto.CreateMeetingRequest;
import org.planb.dto.CreateSubtaskRequest;
import org.planb.model.ProjectRole;
import org.planb.model.TaskPriority;
import org.planb.repository.ProjectMembershipRepository;
import org.planb.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class TaskReadCreateService {
    private final JdbcTemplate jdbc;
    private final ProjectService projects;
    private final UserRepository users;
    private final ProjectMembershipRepository memberships;

    public TaskReadCreateService(JdbcTemplate jdbc, ProjectService projects,
                                UserRepository users, ProjectMembershipRepository memberships) {
        this.jdbc = jdbc;
        this.projects = projects;
        this.users = users;
        this.memberships = memberships;
    }

    private void requireLeader(Long projectId, String email) {
        projects.getProjectDetails(projectId, email); // 404 for outsiders, as specified.
        Long userId = users.findByEmail(email).orElseThrow(() -> new ApiException(401, "token wrong")).getId();
        if (memberships.findByProjectIdAndUserId(projectId, userId).orElseThrow().getRole() != ProjectRole.PROJECT_LEADER) {
            throw new ApiException(403, "no permission");
        }
    }

    @Transactional
    public Long createMeeting(Long projectId, CreateMeetingRequest request, String email) {
        requireLeader(projectId, email);
        if (!request.endDateTime().isAfter(request.startDateTime())) throw new ApiException(400, "end before start");
        if (new HashSet<>(request.participantIds()).size() != request.participantIds().size()) {
            throw new ApiException(400, "duplicate participant IDs");
        }
        for (Long id : request.participantIds()) {
            if (!memberships.existsByProjectIdAndUserId(projectId, id)) {
                throw new ApiException(400, "participant is not a project member");
            }
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO meetings(project_id, title, description, starts_at, ends_at, is_completed)
                VALUES (?, ?, ?, ?, ?, false) RETURNING meeting_id
                """, Long.class, projectId, request.title().trim(), request.description(),
                request.startDateTime(), request.endDateTime());
        for (Long userId : request.participantIds()) {
            jdbc.update("INSERT INTO meeting_participants(meeting_id, user_id) VALUES (?, ?)", id, userId);
        }
        return id;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> meeting(Long meetingId, String email) {
        Long projectId = projectId("SELECT project_id FROM meetings WHERE meeting_id = ?", meetingId, "meeting not found");
        Map<String, Object> meeting = projects.getProjectMeetings(projectId, email).stream()
                .filter(m -> ((Number) m.get("meetingId")).longValue() == meetingId)
                .findFirst().orElseThrow(() -> new ApiException(404, "meeting not found"));
        meeting.put("start", meeting.get("startDateTime"));
        meeting.put("end", meeting.get("endDateTime"));
        return meeting;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> longTodo(Long todoId, String email) {
        Long projectId = projectId("SELECT project_id FROM long_todos WHERE todo_id = ?", todoId, "long to-do not found");
        Map<String, Object> todo = projects.getProjectLongToDos(projectId, email).stream()
                .filter(t -> ((Number) t.get("longToDoId")).longValue() == todoId)
                .findFirst().orElseThrow(() -> new ApiException(404, "long to-do not found"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) todo.get("subtasks");
        for (Map<String, Object> subtask : subtasks) {
            Object assigneeId = subtask.get("assigneeId");
            subtask.put("assigneeFirstName", null);
            subtask.put("assigneeLastName", null);
            if (assigneeId instanceof Number id) users.findById(id.longValue()).ifPresent(user -> {
                subtask.put("assigneeFirstName", user.getFirstname());
                subtask.put("assigneeLastName", user.getLastname());
            });
        }
        return todo;
    }

    @Transactional
    public Long createSubtask(Long todoId, CreateSubtaskRequest request, String email) {
        Long projectId = projectId("SELECT project_id FROM long_todos WHERE todo_id = ?", todoId, "long to-do not found");
        requireLeader(projectId, email);
        Long id = jdbc.queryForObject("""
                INSERT INTO subtasks(todo_id, title, description, priority, is_completed)
                VALUES (?, ?, ?, ?, false) RETURNING subtask_id
                """, Long.class, todoId, request.getTitle().trim(), request.getDescription(),
                (request.getPriority() == null ? TaskPriority.MEDIUM : request.getPriority()).name());
        jdbc.update("UPDATE long_todos SET is_completed = false WHERE todo_id = ?", todoId);
        return id;
    }

    private Long projectId(String sql, Long id, String message) {
        return jdbc.queryForList(sql, Long.class, id).stream()
                .findFirst().orElseThrow(() -> new ApiException(404, message));
    }
}
