package org.planb.controller;

import org.planb.dto.LongToDosResponse;
import org.planb.dto.UpdateLongToDoDone;
import org.planb.dto.UpdateSubtaskDoneRequest;
import org.planb.service.ProjectService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import java.util.HashMap;
import java.util.Map;

 @RestController
 @RequestMapping("/long-todos")
public class LongToDoController {
   
    private final ProjectService projectService;

    public LongToDoController(ProjectService projectService){
        this.projectService = projectService;

    }

    @PutMapping("/{longToDoId}/subtasks/{subtaskId}")
    public ResponseEntity<LongToDosResponse> setSubtaskDone(@PathVariable Long longToDoId, @PathVariable Long subtaskId, @Valid @RequestBody UpdateSubtaskDoneRequest request, Authentication authentication) {
        String email = authentication.getName();
        LongToDosResponse response = projectService.setSubtaskDone(longToDoId, subtaskId, request, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{longToDoId}/done")
    public ResponseEntity<LongToDosResponse> setLongToDoDone(@PathVariable Long longToDoId, @Valid @RequestBody UpdateLongToDoDone request, Authentication authentication) {
        String email = authentication.getName();
        LongToDosResponse response = projectService.setLongToDoDone(longToDoId, request.getDone(), email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{longToDoId}/subtasks/{subtaskId}/claim")
    public ResponseEntity<?> claimSubtask(
            @PathVariable Long longToDoId,
            @PathVariable Long subtaskId,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.claimSubtask(
                    longToDoId, subtaskId, authentication.getName()));
        } catch (ProjectService.SubtaskAlreadyAssignedException exception) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Subtask wurde bereits übernommen.");
            response.put("assigneeId", exception.getAssigneeId());
            return ResponseEntity.status(409).body(response);
        } catch (SecurityException exception) {
            return ResponseEntity.status(403).body(exception.getMessage());
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(404).body(exception.getMessage());
        }
    }
}
