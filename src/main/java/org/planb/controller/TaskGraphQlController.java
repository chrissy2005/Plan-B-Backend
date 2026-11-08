package org.planb.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;

import jakarta.validation.Validator;

import org.planb.dto.CreateLongToDoRequest;
import org.planb.dto.CreateLongToDoResponse;
import org.planb.dto.CreateMeetingRequest;
import org.planb.dto.CreateMeetingResponse;
import org.planb.dto.CreateSubtaskRequest;
import org.planb.model.TaskPriority;
import org.planb.service.MeetingService;
import org.planb.service.ProjectService;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class TaskGraphQlController {

    private final MeetingService meetingService;
    private final ProjectService projectService;
    private final Validator validator;

    public TaskGraphQlController(
            MeetingService meetingService,
            ProjectService projectService,
            Validator validator) {

        this.meetingService = meetingService;
        this.projectService = projectService;
        this.validator = validator;
    }

    public record MeetingInput(
            String title,
            String description,
            String startDateTime,
            String endDateTime,
            List<Long> participantIds) {
    }

    public record LongToDoInput(
            String title,
            String description,
            String startDateTime,
            String endDateTime,
            TaskPriority priority,
            List<Long> assignedMemberIds,
            List<SubtaskInput> subtasks) {
    }

    public record SubtaskInput(
            String title,
            String description,
            TaskPriority priority) {
    }

    @MutationMapping
    @Transactional
    public CreateMeetingResponse createMeeting(
            @Argument("projectId") Long projectId,
            @Argument("input") MeetingInput input,
            Principal principal) {

        String email = requireEmail(principal);
        requireProjectId(projectId);
        requireMembers(input.participantIds());

        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle(trim(input.title()));
        request.setDescription(trim(input.description()));
        request.setStartDateTime(
                parseDateTime(input.startDateTime(), "Start"));
        request.setEndDateTime(
                parseDateTime(input.endDateTime(), "Ende"));
        request.setParticipantIds(input.participantIds());

        validate(request);
        requireTimeOrder(
                request.getStartDateTime(),
                request.getEndDateTime());

        return meetingService.createMeeting(projectId, request, email);
    }

    @MutationMapping
    @Transactional
    public CreateLongToDoResponse createLongToDo(
            @Argument("projectId") Long projectId,
            @Argument("input") LongToDoInput input,
            Principal principal) {

        String email = requireEmail(principal);
        requireProjectId(projectId);
        requireMembers(input.assignedMemberIds());

        CreateLongToDoRequest request = new CreateLongToDoRequest();
        request.setTitle(trim(input.title()));
        request.setDescription(trim(input.description()));
        request.setStartDateTime(
                parseDateTime(input.startDateTime(), "Start"));
        request.setEndDateTime(
                parseDateTime(input.endDateTime(), "Frist"));
        request.setPriority(input.priority());
        request.setAssignedMemberIds(input.assignedMemberIds());

        List<SubtaskInput> subtasks = input.subtasks() == null
                ? List.of()
                : input.subtasks();

        request.setSubtasks(
                subtasks.stream()
                        .map(this::toSubtaskRequest)
                        .toList());

        
        validate(request);
        requireTimeOrder(
                request.getStartDateTime(),
                request.getEndDateTime());

        return projectService.createLongToDo(projectId, request, email);
    }

    private CreateSubtaskRequest toSubtaskRequest(SubtaskInput input) {
        if (input.priority() == null) {
            throw new IllegalArgumentException(
                    "Bitte für jede Teilaufgabe eine Priorität auswählen.");
        }

        CreateSubtaskRequest request = new CreateSubtaskRequest();
        request.setTitle(trim(input.title()));
        request.setDescription(trim(input.description()));
        request.setPriority(input.priority());

        
        request.setAssigneeId(null);
        return request;
    }

    private void validate(Object request) {
        var violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation ->
                            violation.getPropertyPath()
                                    + ": "
                                    + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("\n"));

            throw new IllegalArgumentException(message);
        }
    }

    private static LocalDateTime parseDateTime(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " muss angegeben werden.");
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    fieldName
                            + " muss ein gültiges Datum mit Uhrzeit enthalten.");
        }
    }

    private static void requireTimeOrder(
            LocalDateTime start,
            LocalDateTime end) {

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Ende beziehungsweise Frist muss nach dem Start liegen.");
        }
    }

    private static void requireMembers(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bitte mindestens ein Mitglied auswählen.");
        }

        if (memberIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException(
                    "Die Mitgliederauswahl enthält eine ungültige ID.");
        }
    }

    private static void requireProjectId(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException(
                    "Bitte ein gültiges Projekt auswählen.");
        }
    }

    private static String requireEmail(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Eine Anmeldung ist erforderlich.");
        }

        return principal.getName();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidInput(
            GraphqlErrorBuilder<?> errorBuilder,
            IllegalArgumentException exception) {

        return errorBuilder
                .errorType(ErrorType.BAD_REQUEST)
                .message(exception.getMessage())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleForbidden(
            GraphqlErrorBuilder<?> errorBuilder,
            SecurityException exception) {

        return errorBuilder
                .errorType(ErrorType.FORBIDDEN)
                .message(
                        "Nur die Projektleitung darf Aufgaben anlegen.")
                .build();
    }
}