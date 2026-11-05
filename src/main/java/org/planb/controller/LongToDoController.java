package org.planb.controller;

import org.planb.dto.LongToDosResponse;
import org.planb.dto.ClaimSubtaskRequest;
import org.planb.dto.AssignSubtaskRequest;
import org.planb.dto.MessageResponse;
import org.planb.dto.UpdateLongToDoDone;
import org.planb.dto.UpdateLongToDoRequest;
import org.planb.dto.UpdateSubtaskDoneRequest;
import org.planb.dto.UpdateSubtaskTimeRequest;
import org.planb.service.ProjectService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
            @RequestBody ClaimSubtaskRequest request,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(projectService.claimSubtask(
                    longToDoId, subtaskId, request, authentication.getName()));
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

    @PutMapping("/{longToDoId}/subtasks/{subtaskId}/assignment")
    public ResponseEntity<?> assignSubtask(
                @PathVariable Long longToDoId,
                @PathVariable Long subtaskId,
                @RequestBody AssignSubtaskRequest request,
                Authentication authentication) {
        try {
            String message = projectService.assignSubtask(
                    longToDoId, subtaskId, request, authentication.getName());
            return ResponseEntity.ok(new MessageResponse(message));
        } catch (ProjectService.SubtaskAlreadyAssignedException exception) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "subtask is already assigned");
            response.put("assigneeId", exception.getAssigneeId());
            return ResponseEntity.status(409).body(response);
        } catch (SecurityException exception) {
            return ResponseEntity.status(403).body(new MessageResponse(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(409).body(new MessageResponse(exception.getMessage()));
        }
    }

    @DeleteMapping ("/{longToDoId}")
    public ResponseEntity<MessageResponse> deleteLongToDo(@PathVariable Long longToDoId, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.deleteLongToDo(longToDoId, email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping ("/{longToDoId}/subtasks/{subtaskId}")
    public ResponseEntity<MessageResponse> deleteSubtask(@PathVariable Long longToDoId, @PathVariable Long subtaskId, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.deleteSubtask(longToDoId, subtaskId, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{longToDoId}")
    public ResponseEntity<MessageResponse> updateLongToDo(@PathVariable Long longToDoId, @RequestBody @Valid UpdateLongToDoRequest request, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.updateLongToDo(longToDoId, request, email);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{longToDoId}/subtasks/{subtaskId}/schedule")
    public ResponseEntity<MessageResponse> updateSubtaskTime(@PathVariable Long longToDoId, @PathVariable Long subtaskId, @RequestBody @Valid UpdateSubtaskTimeRequest request, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.updateSubtaskTime(longToDoId, subtaskId, request, email);
        
        return ResponseEntity.ok(response);
    }
}
