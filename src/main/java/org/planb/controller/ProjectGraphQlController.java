package org.planb.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.planb.dto.LongToDosResponse;
import org.planb.dto.MeetingsResponse;
import org.planb.dto.ProjectDetailsResponse;
import org.planb.dto.ProjectMemberResponse;
import org.planb.dto.ProjectsViewResponse;
import org.planb.service.MeetingService;
import org.planb.service.ProjectService;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectGraphQlController {

    private final ProjectService projectService;
    private final MeetingService meetingService;

    public ProjectGraphQlController(
            ProjectService projectService,
            MeetingService meetingService) {
        this.projectService = projectService;
        this.meetingService = meetingService;
    }

    @QueryMapping
    public List<ProjectsViewResponse> myProjects(Principal principal) {
        return projectService.getProjectView(requireEmail(principal));
    }

    @QueryMapping
    public ProjectDetailsResponse project(
            @Argument("projectId") Long projectId,
            Principal principal) {

        String email = requireEmail(principal);

        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException(
                    "A valid project ID is required"
            );
        }

        return projectService.getProjectDetails(projectId, email);
    }

    @SchemaMapping(typeName = "ProjectDetails", field = "members")
    public List<ProjectMemberResponse> members(
            ProjectDetailsResponse project,
            Principal principal) {

        return projectService.getProjectMembers(
                project.getProjectId(),
                requireEmail(principal)
        );
    }

    @SchemaMapping(typeName = "ProjectDetails", field = "meetings")
    public List<MeetingsResponse> meetings(
            ProjectDetailsResponse project,
            Principal principal) {

        return meetingService.getProjectMeetings(
                project.getProjectId(),
                requireEmail(principal)
        );
    }

    @SchemaMapping(typeName = "ProjectDetails", field = "longToDos")
    public List<LongToDosResponse> longToDos(
            ProjectDetailsResponse project,
            Principal principal) {

        return projectService.getLongToDos(
                project.getProjectId(),
                requireEmail(principal)
        );
    }

    @SchemaMapping(
            typeName = "ProjectMeeting",
            field = "startDateTime"
    )
    public String meetingStartDateTime(MeetingsResponse meeting) {
        return formatDateTime(meeting.getStartDateTime());
    }

    @SchemaMapping(
            typeName = "ProjectMeeting",
            field = "endDateTime"
    )
    public String meetingEndDateTime(MeetingsResponse meeting) {
        return formatDateTime(meeting.getEndDateTime());
    }

    @SchemaMapping(
            typeName = "ProjectLongToDo",
            field = "startDateTime"
    )
    public String longToDoStartDateTime(LongToDosResponse longToDo) {
        return formatDateTime(longToDo.getStartDateTime());
    }

    @SchemaMapping(
            typeName = "ProjectLongToDo",
            field = "endDateTime"
    )
    public String longToDoEndDateTime(LongToDosResponse longToDo) {
        return formatDateTime(longToDo.getEndDateTime());
    }

    private String requireEmail(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication is required"
            );
        }

        return principal.getName();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null
                ? null
                : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}