package org.planb.controller;

import jakarta.validation.Valid;
import org.planb.dto.CreateMeetingRequest;
import org.planb.dto.CreateSubtaskRequest;
import org.planb.service.TaskReadCreateService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class TaskReadCreateController {
    private final TaskReadCreateService tasks;
    public TaskReadCreateController(TaskReadCreateService tasks) { this.tasks = tasks; }

    @PostMapping("/projects/{projectId}/meetings")
    public Map<String, Object> createMeeting(@PathVariable Long projectId,
            @Valid @RequestBody CreateMeetingRequest request, Authentication authentication) {
        return Map.of("message", "meeting created", "meetingId", tasks.createMeeting(projectId, request, authentication.getName()));
    }

    @GetMapping("/meetings/{meetingId}")
    public Map<String, Object> meeting(@PathVariable Long meetingId, Authentication authentication) {
        return tasks.meeting(meetingId, authentication.getName());
    }

    @GetMapping("/long-todos/{longToDoId}")
    public Map<String, Object> longTodo(@PathVariable Long longToDoId, Authentication authentication) {
        return tasks.longTodo(longToDoId, authentication.getName());
    }

    @PostMapping("/long-todos/{longToDoId}/subtasks")
    public Map<String, Object> createSubtask(@PathVariable Long longToDoId,
            @Valid @RequestBody CreateSubtaskRequest request, Authentication authentication) {
        return Map.of("message", "subtask created", "subtaskId", tasks.createSubtask(longToDoId, request, authentication.getName()));
    }
}
