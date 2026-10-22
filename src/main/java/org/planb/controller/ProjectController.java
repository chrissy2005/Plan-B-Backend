package org.planb.controller;

import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.ProjectResponse;
import org.planb.dto.MessageResponse;
import org.planb.dto.ProjectDetailsResponse;
import org.planb.dto.ProjectsViewResponse;
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
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

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
    
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailsResponse> getProjectDetails(@PathVariable Long projectId, Authentication authentication) {
        
        String email = authentication.getName();

        ProjectDetailsResponse response = projectService.getProjectDetails(projectId, email);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<ProjectsViewResponse>> getProjectView(Authentication authentication) {
        
        String email = authentication.getName();

        List<ProjectsViewResponse> response = projectService.getProjectView(email);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<Map<String, Object>>> getMembers(
            @PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(projectService.getProjectMembers(projectId, authentication.getName()));
    }

    @GetMapping("/{projectId}/meetings")
    public ResponseEntity<List<Map<String, Object>>> getMeetings(
            @PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(projectService.getProjectMeetings(projectId, authentication.getName()));
    }

    @GetMapping("/{projectId}/long-todos")
    public ResponseEntity<List<Map<String, Object>>> getLongToDos(
            @PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(projectService.getProjectLongToDos(projectId, authentication.getName()));
    }
}
