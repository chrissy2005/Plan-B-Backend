package org.planb.service;

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
import org.planb.repository.ProjectMembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set; //KI Log 43
import java.util.HashSet; //KI Log 43


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
}