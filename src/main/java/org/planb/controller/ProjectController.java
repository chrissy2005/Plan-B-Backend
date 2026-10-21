package org.planb.controller;

import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.ProjectResponse;
import org.planb.dto.MessageResponse;
import org.planb.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//import io.jsonwebtoken.security.Message;

@RestController 
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    public ProjectController(ProjectService projectService){
        this.projectService = projectService;
    }

    @PostMapping 
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request, Authentication authentication) {

        String creatorEmail = authentication.getName();
        ProjectResponse response = projectService.createProject(request, creatorEmail);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationError(MethodArgumentNotValidException exception){
        String errorMessage = exception
                              .getBindingResult()
                              .getFieldErrors()
                              .get(0)
                              .getDefaultMessage();
        return ResponseEntity.badRequest().body(new MessageResponse(errorMessage));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<MessageResponse> addMember(@PathVariable Long projectId, @RequestBody @Valid AddProjectMemberRequest request, Authentication authentication) {
        
        String creatorEmail = authentication.getName();
        projectService.addMember(projectId, request, creatorEmail);
        return ResponseEntity.ok(new MessageResponse("member added"));
    }
    
}
