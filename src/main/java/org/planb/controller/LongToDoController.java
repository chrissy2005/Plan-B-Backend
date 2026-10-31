package org.planb.controller;

import org.planb.dto.LongToDosResponse;
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
}
