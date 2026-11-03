package org.planb.service;

import org.planb.model.LongToDo;
import org.planb.model.Meeting;
import org.planb.model.MeetingParticipants;
import org.planb.model.Project;
import org.planb.model.User;
import org.planb.model.ProjectRole;
import org.planb.model.ProjectMembership;
import org.planb.repository.ProjectRepository;
import org.planb.repository.UserRepository;
import org.planb.repository.MeetingRepository;
import org.planb.repository.MeetingParticipantsRepository;
import org.planb.dto.CreateMeetingRequest;
import org.planb.dto.CreateMeetingResponse;
import org.planb.dto.MeetingsResponse;
import org.planb.dto.MessageResponse;
import org.planb.dto.UpdateMeetingDoneRequest;
import org.planb.repository.ProjectMembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet; 


@Service
public class MeetingService{

    private final MeetingRepository meetingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final UserRepository userRepository;
    private final MeetingParticipantsRepository meetingParticipantsRepository; 

    public MeetingService (MeetingRepository meetingRepository, ProjectRepository projectRepository, ProjectMembershipRepository projectMembershipRepository, UserRepository userRepository, MeetingParticipantsRepository meetingParticipantsRepository){
        this.meetingRepository = meetingRepository;
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.userRepository = userRepository;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
    }

@Transactional
public CreateMeetingResponse createMeeting (Long projectId, CreateMeetingRequest request, String creatorEmail){
     if(!request.getEndDateTime().isAfter(request.getStartDateTime())){ 
            throw new IllegalArgumentException("End date must be after start date");
        }

        Project project = projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));

        User currentUser = userRepository.findByEmail(creatorEmail).orElseThrow(() -> new IllegalStateException("User not found"));

        ProjectMembership currentMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("No project membership"));
    
        if(currentMembership.getRole() != ProjectRole.PROJECT_LEADER){
            throw new SecurityException("No permission to create a meeting");
        }

        List<Long> participantIds = request.getParticipantIds() == null
                                                ? List.of()
                                                : request.getParticipantIds();
        
        Set<Long> uniqueParticipantIds = new HashSet<>(participantIds);
        if(uniqueParticipantIds.size() != participantIds.size()){
            throw new IllegalArgumentException("Duplicate participants Ids ");
        }

        for(Long userId : uniqueParticipantIds){
            boolean isProjectMember = projectMembershipRepository.existsByProjectIdAndUserId(projectId, userId);
            if(!isProjectMember){
                throw new IllegalArgumentException("User is not a project member: " + userId);
            }
        }

       Meeting meeting = new Meeting();
        meeting.setProjectId(projectId);
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setStartDateTime(request.getStartDateTime());
        meeting.setEndDateTime(request.getEndDateTime());
        meeting.setDone(false);

        Meeting savedMeeting = meetingRepository.save(meeting);
        Long meetingId = savedMeeting.getMeetingId();

        for(Long userId : uniqueParticipantIds){
            MeetingParticipants participant= new MeetingParticipants();
            participant.setMeetingId(meetingId);
            participant.setUserId(userId);
            meetingParticipantsRepository.save(participant);
        }

        return new CreateMeetingResponse("meeting created", meetingId);


}

@Transactional(readOnly = true)
public List<MeetingsResponse> getProjectMeetings(Long projectId, String email){
    User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("Project not found"));
       
        List<Meeting> meetings = meetingRepository.findAllByProjectId(projectId);

        if(membership.getRole() == ProjectRole.MEMBER){
            meetings = meetings.stream().filter(meeting -> meetingParticipantsRepository.existsByMeetingIdAndUserId(meeting.getMeetingId(), currentUser.getId()))
                        .toList();
        }

        return meetings.stream().map(this::toMeetingsResponse).toList();
}

private MeetingsResponse toMeetingsResponse(Meeting meeting){
    List<Long> participantIds = meetingParticipantsRepository.findAllByMeetingId(meeting.getMeetingId()).stream().map(MeetingParticipants::getUserId).toList();
    return new MeetingsResponse(meeting.getMeetingId(), meeting.getTitle(), meeting.getDescription(), meeting.getStartDateTime(), meeting.getEndDateTime(), meeting.isDone(), participantIds);
}

@Transactional 
public MessageResponse deleteMeeting(Long meetingId, String email){
    User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(() -> new IllegalStateException("Meeting not found"));
    ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(meeting.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));

    boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
    if(!isProjectLeader){
        throw new IllegalStateException("Only a project leader can delete a meeting");
    }
    meetingParticipantsRepository.deleteAllByMeetingId(meetingId);
    meetingRepository.deleteById(meetingId);
    return new MessageResponse("meeting deleted");
    }

@Transactional 
public MessageResponse updateMeetingDone(Long meetingId, UpdateMeetingDoneRequest request, String email){
    if(request == null || request.getUpdate().getNewValue() == null){
        throw new IllegalArgumentException("Update 'data' is required");
    }
    UpdateMeetingDoneRequest.UpdateField update = request.getUpdate();
    if(!"done".equalsIgnoreCase(update.getAttribute())){
        throw new IllegalArgumentException("Only 'done' attribute can be updated");
    }
    if(update.getNewValue() == null){
        throw new IllegalArgumentException("Field 'done' is required");
    }
    User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(() -> new IllegalStateException("Meeting not found"));
    ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(meeting.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));

    boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
    if(!isProjectLeader){
        throw new IllegalStateException("Only a project leader can update the done status of a meeting");
    }

    meeting.setDone(update.getNewValue());
    meetingRepository.save(meeting);
    return new MessageResponse("meeting status updated");
}
}

/*
        if(done == null ){
            throw new IllegalArgumentException("Field 'done' is required");
        }
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Long todo not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        List<Subtask> subtasks = subtaskRepository.findByLongToDoId(longToDoId);
        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        boolean hasOpenSubtasks = subtasks.stream().anyMatch(subtask -> !subtask.isDone());
        if(!done && !isProjectLeader){
            throw new IllegalStateException("Only a project leader can reopen a long todo");
        }
        if(done && !isProjectLeader && hasOpenSubtasks){
            throw new IllegalStateException("Only a project leader can directly mark a long-todo as done while subtasks are still open.");
        }
        if(isProjectLeader){
            for (Subtask subtask : subtasks){
                if(subtask.isDone() != done){
                    subtask.setDone(done);
                    subtaskRepository.save(subtask);
                }
            }
        }
        longToDo.setDone(done);
        longToDoRepository.save(longToDo);
        LongToDo updatedLongToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Updated long-todo not found"));
        return toLongToDosResponse(updatedLongToDo); */