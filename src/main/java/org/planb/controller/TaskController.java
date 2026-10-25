package org.planb.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.planb.dto.TaskRequest;
import org.planb.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {
    private final TaskService tasks;
    public TaskController(TaskService tasks) { this.tasks = tasks; }

    @GetMapping("/projects/{projectId}/tasks")
    public List<Map<String, Object>> list(@PathVariable Long projectId, Authentication auth) {
        return tasks.list(projectId, auth.getName());
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<Map<String, Object>> create(@PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request, Authentication auth) {
        return ResponseEntity.status(201).body(tasks.create(projectId, request, auth.getName()));
    }

    @GetMapping("/projects/{projectId}/tasks/{taskId}")
    public Map<String, Object> get(@PathVariable Long projectId, @PathVariable String taskId, Authentication auth) {
        return tasks.get(projectId, taskId, auth.getName());
    }

    @PutMapping("/projects/{projectId}/tasks/{taskId}")
    public Map<String, Object> update(@PathVariable Long projectId, @PathVariable String taskId,
            @Valid @RequestBody TaskRequest request, Authentication auth) {
        return tasks.update(projectId, taskId, request, auth.getName());
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable String taskId, Authentication auth) {
        tasks.delete(projectId, taskId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/projects/{projectId}/tasks/{taskId}/assignees/me")
    public Map<String, Object> assign(@PathVariable Long projectId, @PathVariable String taskId, Authentication auth) {
        return tasks.assignSelf(projectId, taskId, auth.getName(), true);
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}/assignees/me")
    public Map<String, Object> unassign(@PathVariable Long projectId, @PathVariable String taskId, Authentication auth) {
        return tasks.assignSelf(projectId, taskId, auth.getName(), false);
    }

    public record StatusRequest(@NotBlank String status) {}

    @PutMapping("/projects/{projectId}/tasks/{taskId}/status")
    public Map<String, Object> status(@PathVariable Long projectId, @PathVariable String taskId,
            @Valid @RequestBody StatusRequest request, Authentication auth) {
        return tasks.status(projectId, taskId, request.status(), auth.getName());
    }

    @GetMapping("/users/me/calendar")
    public List<Map<String, Object>> calendar(Authentication auth) {
        return tasks.calendar(auth.getName());
    }

    @PostMapping("/tasks/{taskId}/subtasks/{subtaskId}/claim")
    public Map<String, Object> claim(@PathVariable String taskId, @PathVariable Long subtaskId, Authentication auth) {
        return tasks.claimSubtask(taskId, subtaskId, auth.getName());
    }

    public record SubtaskStatusRequest(@jakarta.validation.constraints.NotNull Boolean done) {}

    @PatchMapping("/tasks/{taskId}/subtasks/{subtaskId}")
    public Map<String, Object> subtaskStatus(@PathVariable String taskId, @PathVariable Long subtaskId,
            @Valid @RequestBody SubtaskStatusRequest request, Authentication auth) {
        return Map.of("task", tasks.subtaskStatus(taskId, subtaskId, request.done(), auth.getName()));
    }
}
