package org.planb.service;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.planb.repository.LongToDoRepository;
import org.planb.repository.MeetingRepository;
import org.planb.repository.ProjectRepository;
import org.planb.repository.SubtaskRepository;
import org.planb.repository.ToDoParticipantsRepository;
import org.planb.repository.MeetingParticipantsRepository;
import org.planb.repository.UserRepository;
import org.planb.dto.DashboardLongToDoResponse;
import org.planb.dto.DashboardSubtaskResponse;
import org.planb.dto.DashboardMeetingResponse;
import org.planb.model.User;
import org.planb.model.TaskPriority;
import org.planb.model.ToDoParticipants;
import org.planb.model.DashboardSubtaskEntry;
import org.planb.model.LongToDo;
import org.planb.model.MeetingParticipants;
import org.planb.model.Meeting;
import org.planb.model.Subtask;
import org.planb.model.Project;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Collections;

@Service 
public class DashboardService {
   
    private final UserRepository userRepository;
    private final LongToDoRepository longToDoRepository;
    private final SubtaskRepository subtaskRepository;
    private final MeetingRepository meetingRepository;
    private final ToDoParticipantsRepository toDoParticipantsRepository;
    private final MeetingParticipantsRepository meetingParticipantsRepository;
    private final ProjectRepository projectRepository;
    private static final ZoneId PROJECT_ZONE = ZoneId.of("Europe/Berlin");

    public DashboardService(UserRepository userRepository, LongToDoRepository longToDoRepository, SubtaskRepository subtaskRepository, MeetingRepository meetingRepository, ToDoParticipantsRepository toDoParticipantsRepository, MeetingParticipantsRepository meetingParticipantsRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.longToDoRepository = longToDoRepository;
        this.subtaskRepository = subtaskRepository;
        this.meetingRepository = meetingRepository;
        this.toDoParticipantsRepository = toDoParticipantsRepository;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
        this.projectRepository = projectRepository;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value){
        return value == null
                ? null
                : value
                    .atZone(PROJECT_ZONE)
                    .toOffsetDateTime();
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value){
        if(value == null){
            return null;
        }
        
        return value.atZoneSameInstant(PROJECT_ZONE).toLocalDateTime();
    }

    @Transactional(readOnly = true)
    public List<DashboardLongToDoResponse> getMyLongToDos(String email, LocalDateTime from, LocalDateTime to, TaskPriority priority, Boolean done) {
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        List<ToDoParticipants> participants = toDoParticipantsRepository.findAllByuserId(currentUser.getId());
        if(participants.isEmpty()){
            return Collections.emptyList();
        }
        return participants.stream()
            .map(ToDoParticipants::getToDoId).filter(id -> id != null).distinct()
            .flatMap(longToDoId -> longToDoRepository.searchForDashboard(longToDoId, priority, done, from, to).stream())
            .map(this::toDashboardLongToDoResponse)
            .toList();     
    }


    @Transactional(readOnly = true)
    public List<DashboardSubtaskResponse> getMySubtasks(String email,TaskPriority priority, Boolean done, LocalDateTime from, LocalDateTime to) {
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        List<ToDoParticipants> participants = toDoParticipantsRepository.findAllByuserId(currentUser.getId());
        if(participants.isEmpty()){
            return Collections.emptyList();
        }

        OffsetDateTime from_off = toOffsetDateTime(from);
        OffsetDateTime to_off = toOffsetDateTime(to);

        return participants.stream()
            .map(ToDoParticipants::getToDoId).filter(id -> id != null).distinct()
            .flatMap(longToDoId -> {
                List<LongToDo> longToDos = longToDoRepository.searchForDashboard(longToDoId, null, null, from, to);
                if(longToDos.isEmpty()){
                    return java.util.stream.Stream.empty();
                }
                LongToDo longToDo = longToDos.get(0);
                return subtaskRepository.searchForDashboard(longToDoId, priority, done, currentUser.getId(), from_off, to_off).stream()
                .map(subtask -> toDashboardSubtaskResponse(subtask, longToDo));
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardMeetingResponse> getMyMeetings(String email, LocalDateTime from, LocalDateTime to, Boolean done) {
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        List<MeetingParticipants> participants = meetingParticipantsRepository.findAllByUserId(currentUser.getId());
        if(participants.isEmpty()){
            return Collections.emptyList();
        }
        return participants.stream()
            .map(MeetingParticipants::getMeetingId).filter(id -> id != null).distinct()
            .flatMap(meetingId -> meetingRepository.searchForDashboard(meetingId,  done,  from, to).stream())
            .map(this::toDashboardMeetingResponse)
            .toList(); 
    }

    private DashboardLongToDoResponse toDashboardLongToDoResponse(LongToDo longToDo) {
        String projectName = findProjectName(longToDo.getProjectId());
        return new DashboardLongToDoResponse(
            longToDo.getToDoId(),
            longToDo.getProjectId(),
            projectName,
            longToDo.getTitle(),
            longToDo.getDescription(),
            longToDo.getStartTime(),
            longToDo.getEndTime(),
            longToDo.getPriority(),
            longToDo.isDone()
        );
    }

    private DashboardSubtaskResponse toDashboardSubtaskResponse(DashboardSubtaskEntry subtask, LongToDo longToDo) {
        String projectName = findProjectName(longToDo.getProjectId());
        return new DashboardSubtaskResponse(
            subtask.getSubtaskId(),
            subtask.getLongToDoId(),
            longToDo.getTitle(),
            longToDo.getProjectId(),
            projectName,
            subtask.getTitle(),
            subtask.getDescription(),
            toLocalDateTime(subtask.getStart()), //eigentlich muss hier subtask.getStartTime() stehen
            toLocalDateTime(subtask.getEnd()),  // eigentlich muss hier subtask.getEndTime() stehen
            subtask.getPriority(),
            subtask.isDone()
        );
    }

    private DashboardMeetingResponse toDashboardMeetingResponse(Meeting meeting) {
        String projectName = findProjectName(meeting.getProjectId());
        return new DashboardMeetingResponse(
            meeting.getMeetingId(),
            meeting.getProjectId(),
            projectName,
            meeting.getTitle(),
            meeting.getDescription(),
            meeting.getStartDateTime(),
            meeting.getEndDateTime(),
            meeting.isDone()
        );
    }


    private String findProjectName(Long projectId) {
        if(projectId == null){
            return null;
        }
        return projectRepository.findByProjectId(projectId)
            .map(Project::getName)
            .orElse(null);
    }
}
