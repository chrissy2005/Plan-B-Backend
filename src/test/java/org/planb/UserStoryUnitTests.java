package org.planb;

// KI-gestützter Testentwurf vom 22.11.2026.
// Konkrete KI-Log-Nummer nach Abstimmung ergänzen.

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import org.planb.dto.*;
import org.planb.model.*;
import org.planb.repository.*;
import org.planb.service.MeetingService;
import org.planb.service.ProjectService;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserStoryUnitTests {

    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final Long TODO_ID = 30L;
    private static final Long SUBTASK_ID = 40L;
    private static final Long MEETING_ID = 50L;
    private static final Long OTHER_USER_ID = 99L;

    private static final String EMAIL = "mitglied@example.test";
    private static final LocalDateTime START =
            LocalDateTime.of(2026, 11, 23, 10, 0);

    private ProjectRepository projects;
    private ProjectMembershipRepository memberships;
    private UserRepository users;
    private LongToDoRepository todos;
    private ToDoParticipantsRepository todoParticipants;
    private SubtaskRepository subtasks;
    private MeetingRepository meetings;
    private MeetingParticipantsRepository meetingParticipants;

    private ProjectService projectService;
    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        memberships = mock(ProjectMembershipRepository.class);
        users = mock(UserRepository.class);
        todos = mock(LongToDoRepository.class);
        todoParticipants = mock(ToDoParticipantsRepository.class);
        subtasks = mock(SubtaskRepository.class);
        meetings = mock(MeetingRepository.class);
        meetingParticipants = mock(MeetingParticipantsRepository.class);

        projectService = new ProjectService(
                projects,
                memberships,
                users,
                mock(JdbcProjectMembershipRepository.class),
                mock(JdbcTemplate.class),
                todos,
                todoParticipants,
                subtasks,
                meetingParticipants,
                meetings
        );

        meetingService = new MeetingService(
                meetings,
                projects,
                memberships,
                users,
                meetingParticipants,
                subtasks
        );
    }

    // US1: Erfolgreiche Aufgabenanlage

    @Test
    void us1_meetingIsCreatedWithCorrectDataAndParticipant() {
        prepareUser(ProjectRole.PROJECT_LEADER);

        when(meetings.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting meeting = invocation.getArgument(0);
            meeting.setMeetingId(MEETING_ID);
            return meeting;
        });

        var response = meetingService.createMeeting(
                PROJECT_ID, validMeeting(), EMAIL
        );

        assertEquals(MEETING_ID, response.getMeetingId());

        var captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetings).save(captor.capture());

        Meeting saved = captor.getValue();
        assertEquals(PROJECT_ID, saved.getProjectId());
        assertEquals("Planung", saved.getTitle());
        assertEquals("Gemeinsame Abstimmung", saved.getDescription());
        assertEquals(START, saved.getStartDateTime());
        assertEquals(START.plusHours(1), saved.getEndDateTime());
        assertFalse(saved.isDone());

        verify(meetingParticipants).save(argThat(participant ->
                MEETING_ID.equals(participant.getMeetingId())
                        && USER_ID.equals(participant.getUserId())
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void us1_longTodoCanBeCreatedWithoutOrWithSubtask(
            boolean withSubtask) {

        prepareUser(ProjectRole.PROJECT_LEADER);
        CreateLongToDoRequest request = validTodo();

        if (withSubtask) {
            CreateSubtaskRequest subtask = new CreateSubtaskRequest();
            subtask.setTitle("Testfälle beschreiben");
            subtask.setDescription("Normalfall und Fehlerfälle");
            subtask.setPriority(TaskPriority.HIGH);
            request.setSubtasks(List.of(subtask));
        }

        when(todos.save(any(LongToDo.class))).thenAnswer(invocation -> {
            LongToDo todo = invocation.getArgument(0);
            todo.setToDoId(TODO_ID);
            return todo;
        });

        var response = projectService.createLongToDo(
                PROJECT_ID, request, EMAIL
        );

        assertEquals(TODO_ID, response.getLongToDoId());

        var captor = ArgumentCaptor.forClass(LongToDo.class);
        verify(todos).save(captor.capture());

        LongToDo saved = captor.getValue();
        assertEquals(PROJECT_ID, saved.getProjectId());
        assertEquals("Qualitätssicherung", saved.getTitle());
        assertEquals("Tests vorbereiten", saved.getDescription());
        assertEquals(START, saved.getStartTime());
        assertEquals(START.plusDays(1), saved.getEndTime());
        assertEquals(TaskPriority.MEDIUM, saved.getPriority());
        assertFalse(saved.isDone());

        verify(todoParticipants).save(argThat(participant ->
                TODO_ID.equals(participant.getToDoId())
                        && USER_ID.equals(participant.getUserId())
        ));

        if (withSubtask) {
            var subtaskCaptor = ArgumentCaptor.forClass(Subtask.class);
            verify(subtasks).save(subtaskCaptor.capture());

            Subtask savedSubtask = subtaskCaptor.getValue();
            assertEquals(TODO_ID, savedSubtask.getLongToDoId());
            assertEquals("Testfälle beschreiben", savedSubtask.getTitle());
            assertEquals(
                    "Normalfall und Fehlerfälle",
                    savedSubtask.getDescription()
            );
            assertEquals(TaskPriority.HIGH, savedSubtask.getPriority());
            assertNull(savedSubtask.getAssigneeId());
            assertFalse(savedSubtask.isDone());
        } else {
            verifyNoInteractions(subtasks);
        }
    }

    // US1: Berechtigungen und ungültige Eingaben

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void us1_normalMemberCannotCreateTasks(boolean meeting) {
        prepareUser(ProjectRole.MEMBER);

        assertThrows(SecurityException.class, () ->
                createTask(meeting, START.plusHours(1), List.of(USER_ID))
        );

        verifyNoTaskWrites();
    }

    @ParameterizedTest
    @CsvSource({
            "true, 0",
            "true, -1",
            "false, 0",
            "false, -1"
    })
    void us1_endMustBeStrictlyAfterStart(
            boolean meeting, int minutesAfterStart) {

        assertThrows(IllegalArgumentException.class, () ->
                createTask(
                        meeting,
                        START.plusMinutes(minutesAfterStart),
                        List.of(USER_ID)
                )
        );

        verifyNoTaskWrites();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void us1_participantsMustBelongToProject(boolean meeting) {
        prepareUser(ProjectRole.PROJECT_LEADER);

        when(memberships.existsByProjectIdAndUserId(
                PROJECT_ID, OTHER_USER_ID
        )).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                createTask(
                        meeting,
                        START.plusHours(1),
                        List.of(OTHER_USER_ID)
                )
        );

        verifyNoTaskWrites();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void us1_duplicateParticipantsAreRejected(boolean meeting) {
        prepareUser(ProjectRole.PROJECT_LEADER);

        assertThrows(IllegalArgumentException.class, () ->
                createTask(
                        meeting,
                        START.plusHours(1),
                        List.of(USER_ID, USER_ID)
                )
        );

        verifyNoTaskWrites();
    }

    // US1: Verfügbarkeitsprüfung
    // Die Überschneidungssuche im SQL muss separat integriert getestet werden.

    @ParameterizedTest
    @CsvSource({
            "false, false, true",
            "true, false, false",
            "false, true, false"
    })
    void us1_availabilityConsidersMeetingsAndSubtasks(
            boolean meetingConflict,
            boolean subtaskConflict,
            boolean expectedAvailable) {

        prepareUser(ProjectRole.PROJECT_LEADER);

        OffsetDateTime from =
                OffsetDateTime.parse("2026-11-23T10:00:00+01:00");
        OffsetDateTime to = from.plusHours(1);

        List<CalendarMeetingEntry> existingMeetings = meetingConflict
                ? List.of(new CalendarMeetingEntry(
                        MEETING_ID, "Belegter Termin", "Testprojekt",
                        from, to, false
                ))
                : List.of();

        List<CalendarSubtaskEntry> existingSubtasks = subtaskConflict
                ? List.of(new CalendarSubtaskEntry(
                        SUBTASK_ID, "Geplante Arbeit", "Testprojekt",
                        "Langes To-Do", from, to, false
                ))
                : List.of();

        when(meetings.findAllByParticipantInPeriod(USER_ID, from, to))
                .thenReturn(existingMeetings);
        when(subtasks.findAllByAssigneeInPeriod(USER_ID, from, to))
                .thenReturn(existingSubtasks);

        AvailabilityResponse response = meetingService.checkAvailability(
                PROJECT_ID,
                new AvailabilityRequest(List.of(USER_ID), from, to),
                EMAIL
        );

        assertEquals(expectedAvailable, response.available());
        assertEquals(
                expectedAvailable ? 0 : 1,
                response.unavailableMembers().size()
        );

        if (!expectedAvailable) {
            assertEquals(
                    USER_ID,
                    response.unavailableMembers().getFirst().userId()
            );
        }
    }

    // US2: Übernahme einer Untertask

    @Test
    void us2_freeSubtaskIsClaimedByAuthenticatedUser() {
        prepareClaim();

        Subtask free = subtask(null);
        Subtask assigned = subtask(USER_ID);

        when(subtasks.findByIdAndLongToDoId(SUBTASK_ID, TODO_ID))
                .thenReturn(Optional.of(free), Optional.of(assigned));
        when(subtasks.claimIfUnassigned(SUBTASK_ID, USER_ID))
                .thenReturn(1);

        Subtask result = projectService.claimSubtask(
                TODO_ID, SUBTASK_ID, new ClaimSubtaskRequest(), EMAIL
        );

        assertEquals(USER_ID, result.getAssigneeId());
        assertEquals(SUBTASK_ID, result.getSubtaskId());

        verify(subtasks).claimIfUnassigned(SUBTASK_ID, USER_ID);

        // Die Übernahme nutzt die gezielte Zuweisung und schreibt
        // nicht das gesamte Aufgabenobjekt neu.
        verify(subtasks, never()).save(any(Subtask.class));
    }

    @Test
    void us2_outsiderCannotClaimSubtask() {
        prepareClaim();

        when(memberships.existsByProjectIdAndUserId(
                PROJECT_ID, USER_ID
        )).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                projectService.claimSubtask(
                        TODO_ID, SUBTASK_ID,
                        new ClaimSubtaskRequest(), EMAIL
                )
        );

        verifyNoInteractions(subtasks);
    }

    @Test
    void us2_subtaskMustBelongToRequestedTodo() {
        prepareClaim();

        when(subtasks.findByIdAndLongToDoId(SUBTASK_ID, TODO_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                projectService.claimSubtask(
                        TODO_ID, SUBTASK_ID,
                        new ClaimSubtaskRequest(), EMAIL
                )
        );

        verify(subtasks, never()).claimIfUnassigned(anyLong(), anyLong());
    }

    @Test
    void us2_existingAssignmentCannotBeOverwritten() {
        prepareClaim();

        when(subtasks.findByIdAndLongToDoId(SUBTASK_ID, TODO_ID))
                .thenReturn(Optional.of(subtask(OTHER_USER_ID)));

        var error = assertThrows(
                ProjectService.SubtaskAlreadyAssignedException.class,
                () -> projectService.claimSubtask(
                        TODO_ID, SUBTASK_ID,
                        new ClaimSubtaskRequest(), EMAIL
                )
        );

        assertEquals(OTHER_USER_ID, error.getAssigneeId());
        verify(subtasks, never()).claimIfUnassigned(anyLong(), anyLong());
    }

    @Test
    void us2_assignmentChangedBetweenReadAndWriteIsReported() {
        prepareClaim();

        when(subtasks.findByIdAndLongToDoId(SUBTASK_ID, TODO_ID))
                .thenReturn(
                        Optional.of(subtask(null)),
                        Optional.of(subtask(OTHER_USER_ID))
                );

        // Die Datenbank meldet: keine freie Zeile mehr geändert.
        when(subtasks.claimIfUnassigned(SUBTASK_ID, USER_ID))
                .thenReturn(0);

        var error = assertThrows(
                ProjectService.SubtaskAlreadyAssignedException.class,
                () -> projectService.claimSubtask(
                        TODO_ID, SUBTASK_ID,
                        new ClaimSubtaskRequest(), EMAIL
                )
        );

        assertEquals(OTHER_USER_ID, error.getAssigneeId());
        verify(subtasks).claimIfUnassigned(SUBTASK_ID, USER_ID);
        verify(subtasks, never()).save(any(Subtask.class));
    }

    // Validierung: tatsächliche Bean-Validation-Regeln der DTOs

    @ParameterizedTest
    @CsvSource({"100, true", "101, false"})
    void us1_meetingTitleLengthBoundary(int length, boolean valid) {
        CreateMeetingRequest request = validMeeting();
        request.setTitle("A".repeat(length));

        assertEquals(valid, violations(request).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"150, true", "151, false"})
    void us1_todoTitleLengthBoundary(int length, boolean valid) {
        CreateLongToDoRequest request = validTodo();
        request.setTitle("A".repeat(length));

        assertEquals(valid, violations(request).isEmpty());
    }

    @Test
    void us1_requiredMeetingFieldsAreValidated() {
        CreateMeetingRequest request = validMeeting();
        request.setTitle(" ");
        request.setStartDateTime(null);
        request.setEndDateTime(null);

        Set<String> fields = violations(request);

        assertTrue(fields.contains("title"));
        assertTrue(fields.contains("startDateTime"));
        assertTrue(fields.contains("endDateTime"));
    }

    @Test
    void us1_requiredTodoFieldsAreValidated() {
        CreateLongToDoRequest request = validTodo();
        request.setTitle(" ");
        request.setStartDateTime(null);
        request.setEndDateTime(null);
        request.setPriority(null);
        request.setAssignedMemberIds(List.of());

        Set<String> fields = violations(request);

        assertTrue(fields.contains("title"));
        assertTrue(fields.contains("startDateTime"));
        assertTrue(fields.contains("endDateTime"));
        assertTrue(fields.contains("priority"));
        assertTrue(fields.contains("assignedMemberIds"));
    }

    @Test
    void us1_subtaskTitleIsValidatedInsideTodoRequest() {
        CreateLongToDoRequest request = validTodo();

        CreateSubtaskRequest child = new CreateSubtaskRequest();
        child.setTitle(" ");
        child.setPriority(TaskPriority.MEDIUM);
        request.setSubtasks(List.of(child));

        assertTrue(violations(request).contains("subtasks[0].title"));
    }

    // Gemeinsame Testdaten und Hilfsmethoden

    private void prepareUser(ProjectRole role) {
        User user = new User(
                USER_ID, "Test", "Mitglied", EMAIL, "unused-test-hash"
        );

        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projects.findByProjectId(PROJECT_ID))
                .thenReturn(Optional.of(new Project()));
        when(memberships.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(
                        new ProjectMembership(PROJECT_ID, USER_ID, role)
                ));
        when(memberships.existsByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(true);
    }

    private void prepareClaim() {
        prepareUser(ProjectRole.MEMBER);

        LongToDo todo = new LongToDo();
        todo.setToDoId(TODO_ID);
        todo.setProjectId(PROJECT_ID);

        when(todos.findById(TODO_ID)).thenReturn(Optional.of(todo));
    }

    private CreateMeetingRequest validMeeting() {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("Planung");
        request.setDescription("Gemeinsame Abstimmung");
        request.setStartDateTime(START);
        request.setEndDateTime(START.plusHours(1));
        request.setParticipantIds(List.of(USER_ID));
        return request;
    }

    private CreateLongToDoRequest validTodo() {
        CreateLongToDoRequest request = new CreateLongToDoRequest();
        request.setTitle("Qualitätssicherung");
        request.setDescription("Tests vorbereiten");
        request.setStartDateTime(START);
        request.setEndDateTime(START.plusDays(1));
        request.setPriority(TaskPriority.MEDIUM);
        request.setAssignedMemberIds(List.of(USER_ID));
        request.setSubtasks(List.of());
        return request;
    }

    private Subtask subtask(Long assigneeId) {
        return new Subtask(
                "Testfälle beschreiben",
                "Normalfall und Fehlerfälle",
                SUBTASK_ID,
                TODO_ID,
                false,
                assigneeId,
                TaskPriority.MEDIUM
        );
    }

    private void createTask(
            boolean meeting,
            LocalDateTime end,
            List<Long> participants) {

        if (meeting) {
            CreateMeetingRequest request = validMeeting();
            request.setEndDateTime(end);
            request.setParticipantIds(participants);
            meetingService.createMeeting(PROJECT_ID, request, EMAIL);
        } else {
            CreateLongToDoRequest request = validTodo();
            request.setEndDateTime(end);
            request.setAssignedMemberIds(participants);
            projectService.createLongToDo(PROJECT_ID, request, EMAIL);
        }
    }

    private void verifyNoTaskWrites() {
        verify(meetings, never()).save(any(Meeting.class));
        verify(todos, never()).save(any(LongToDo.class));
        verify(subtasks, never()).save(any(Subtask.class));
        verifyNoInteractions(meetingParticipants, todoParticipants);
    }

    private Set<String> violations(Object request) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
        }
    }
}