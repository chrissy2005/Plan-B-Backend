package org.planb.controller;

import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.AvailabilityResponse;
import org.planb.dto.CreateLongToDoRequest;
import org.planb.dto.CreateLongToDoResponse;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.CreateMeetingRequest;
import org.planb.dto.LongToDosResponse;
import org.planb.dto.MeetingsResponse;
import org.planb.dto.ProjectResponse;
import org.planb.dto.MessageResponse;
import org.planb.dto.ProjectDetailsResponse;
import org.planb.dto.ProjectMemberResponse;
import org.planb.dto.ProjectsViewResponse;
import org.planb.dto.CreateMeetingResponse;
import org.planb.dto.UpdateProjectMemberRoleRequest;
import org.planb.dto.UpdateProjectRequest;
import org.planb.dto.AvailabilityRequest;
import org.planb.service.ProjectService;
import org.planb.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import org.springframework.web.bind.annotation.PutMapping;




@RestController 
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final MeetingService meetingService;
    public ProjectController(ProjectService projectService, MeetingService meetingService){
        this.projectService = projectService;
        this.meetingService = meetingService;
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
        try {
            projectService.addMember(projectId, request, creatorEmail);
            return ResponseEntity.ok(new MessageResponse("member added"));
        } catch (SecurityException exception) {
            return ResponseEntity.status(403).body(new MessageResponse("no permission"));
        } catch (IllegalStateException exception) {
            return memberError(exception);
        }
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
    public ResponseEntity<?> getProjectmembers( @PathVariable Long projectId, Authentication authentication){
        String email = authentication.getName();
        try {
            List<ProjectMemberResponse> members = projectService.getProjectMembers(projectId, email);
            return ResponseEntity.ok(members);
        } catch (IllegalStateException exception) {
            return memberError(exception);
        }
    }

    private ResponseEntity<MessageResponse> memberError(IllegalStateException exception) {
        String message = exception.getMessage();
        if ("user is already member".equals(message)) {
            return ResponseEntity.badRequest().body(new MessageResponse(message));
        }
        if ("project not found".equalsIgnoreCase(message) || "user not found".equalsIgnoreCase(message)) {
            return ResponseEntity.status(404).body(new MessageResponse(message.toLowerCase(java.util.Locale.ROOT)));
        }
        throw exception;
    }

    

    @GetMapping("/{projectId}/meetings")
    public ResponseEntity<List<MeetingsResponse>> getMeetings(@PathVariable Long projectId, Authentication authentication) {
        String creatorEmail = authentication.getName();
        List<MeetingsResponse> meetings = meetingService.getProjectMeetings(projectId, creatorEmail);
        return ResponseEntity.ok(meetings);
    }

     @PostMapping("/{projectId}/meetings")
    public ResponseEntity<CreateMeetingResponse> createMeeting(@PathVariable Long projectId, @RequestBody @Valid CreateMeetingRequest request, Authentication authentication) {
        String creatorEmail = authentication.getName();
        CreateMeetingResponse response = meetingService.createMeeting(projectId, request, creatorEmail);
        return ResponseEntity.ok(response);
        
    }

    @GetMapping("/{projectId}/long-todos")
    public ResponseEntity<List<LongToDosResponse>> getLongToDos(@PathVariable Long projectId, Authentication authentication) {
        String email = authentication.getName();
        List<LongToDosResponse> longToDos = projectService.getLongToDos(projectId, email);
        return ResponseEntity.ok(longToDos);
    }

    @PostMapping("/{projectId}/long-todos")
    public ResponseEntity<CreateLongToDoResponse> createLongToDo(@PathVariable Long projectId, @RequestBody @Valid CreateLongToDoRequest request, Authentication authentication) {
        String creatorEmail = authentication.getName();
        CreateLongToDoResponse response = projectService.createLongToDo(projectId, request, creatorEmail);
        return ResponseEntity.ok(response);
        
    }

    @DeleteMapping ("/{projectId}/members/{userId}")
    public ResponseEntity<MessageResponse> deleteMember(@PathVariable Long projectId, @PathVariable Long userId, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.deleteMember(projectId, userId, email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping ("/{projectId}")
    public ResponseEntity<MessageResponse> deleteProject(@PathVariable Long projectId, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.deleteProject(projectId, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}/members/{userId}")
    public ResponseEntity<MessageResponse> updateProjectMemberRole(@PathVariable Long projectId, @PathVariable Long userId, @RequestBody @Valid UpdateProjectMemberRoleRequest request, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.updateProjectMemberRole(projectId, userId, request, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<MessageResponse> updateProject(@PathVariable Long projectId, @RequestBody @Valid UpdateProjectRequest request, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = projectService.updateProject(projectId, request, email);
        return ResponseEntity.ok(response);  
    }

    @PostMapping("/{projectId}/availability")
    public ResponseEntity<AvailabilityResponse> postMethodName(@PathVariable Long projectId, @RequestBody @Valid AvailabilityRequest request, Authentication authentication) {
        String email = authentication.getName();
        AvailabilityResponse response = meetingService.checkAvailability(projectId, request, email);
        
        return ResponseEntity.ok(response);
    }
    

}
