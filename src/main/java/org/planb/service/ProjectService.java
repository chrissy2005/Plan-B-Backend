package org.planb.service;

import org.planb.dto.ProjectResponse;
import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.CreateLongToDoRequest;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.CreateSubtaskRequest;
import org.planb.dto.LongToDosResponse;
import org.planb.dto.MessageResponse;
import org.planb.dto.ProjectDetailsResponse;
import org.planb.dto.ProjectMemberResponse;
import org.planb.dto.ProjectsViewResponse;
import org.planb.dto.SubtaskResponse;
import org.planb.dto.UpdateLongToDoRequest;
import org.planb.dto.UpdateSubtaskDoneRequest;
import org.planb.dto.CreateLongToDoResponse;
import org.planb.dto.ClaimSubtaskRequest;
import org.planb.dto.AssignSubtaskRequest;
import org.planb.dto.UpdateProjectMemberRoleRequest;
import org.planb.dto.UpdateProjectRequest;
import org.planb.dto.UpdateSubtaskRequest;
import org.planb.dto.UpdateSubtaskTimeRequest;
import org.planb.model.LongToDo;
import org.planb.model.Project;
import org.planb.model.ProjectMembership;
import org.planb.model.ProjectRole;
import org.planb.model.ProjectStatus;
import org.planb.model.Subtask;
import org.planb.model.ToDoParticipants;
import org.planb.model.User;
import org.planb.model.Meeting;
import org.planb.repository.MeetingRepository;
import org.planb.repository.MeetingParticipantsRepository;
import org.planb.repository.JdbcProjectMembershipRepository;
import org.planb.repository.LongToDoRepository;
import org.planb.repository.ProjectMembershipRepository;
import org.planb.repository.ProjectRepository;
import org.planb.repository.SubtaskRepository;
import org.planb.repository.ToDoParticipantsRepository;
import org.planb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;


import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final LongToDoRepository longToDoRepository;
    private final ToDoParticipantsRepository toDoParticipantsRepository;
    private final SubtaskRepository subtaskRepository;
    private final MeetingParticipantsRepository meetingParticipantsRepository;
    private final MeetingRepository meetingRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMembershipRepository projectMembershipRepository,
                          UserRepository userRepository,
                          JdbcProjectMembershipRepository jdbcProjectMembershipRepository,
                          JdbcTemplate jdbcTemplate,
                          LongToDoRepository longToDoRepository,
                          ToDoParticipantsRepository toDoParticipantsRepository,
                          SubtaskRepository subtaskRepository,
                          MeetingParticipantsRepository meetingParticipantsRepository,
                          MeetingRepository meetingRepository){
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.longToDoRepository = longToDoRepository;
        this.toDoParticipantsRepository = toDoParticipantsRepository;
        this.subtaskRepository = subtaskRepository;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
        this.meetingRepository = meetingRepository;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, String creatorEmail){
        User creator = userRepository.findByEmail(creatorEmail).orElseThrow(() -> new IllegalStateException("Authenticated user could not be found"));
        String name = request.getName().trim();
        String description = request.getDescription();
        if(description != null){
            description.trim();
        }
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setCreatedByUserId(creator.getId());
        project.setProjectStatus(ProjectStatus.ACTIVE);

        Project savedProject = projectRepository.save(project);

        if(savedProject == null || savedProject.getId() == null) {
            throw new IllegalStateException("Project could not be saved");
        }

        ProjectMembership membership = new ProjectMembership(savedProject.getId(), creator.getId(), ProjectRole.PROJECT_LEADER);
        projectMembershipRepository.save(membership);

        return new ProjectResponse("project created", savedProject.getId(), savedProject.getName(), savedProject.getDescription(), savedProject.getCreatedByUserId());

    }

    @Transactional
    public void addMember(Long projectId, AddProjectMemberRequest request, String requesterEmail){
        projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("project not found"));

        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new IllegalStateException("authenticated user could not be found"));

        ProjectMembership requesterMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, requester.getId())
                                                .orElseThrow(() -> new SecurityException("no permission"));

        if(requesterMembership.getRole() != ProjectRole.PROJECT_LEADER){
            throw new SecurityException("no permission");
        }

        User newMember = userRepository.findByEmail(request.getEmail().trim()).orElseThrow(() -> new IllegalStateException("user not found"));

        if(projectMembershipRepository.existsByProjectIdAndUserId(projectId, newMember.getId())){
            throw new IllegalStateException("user is already member");
        }


        ProjectMembership membership = new ProjectMembership(projectId, newMember.getId(), request.getRole());

        if (!projectMembershipRepository.addIfAbsent(membership)) {
            throw new IllegalStateException("user is already member");
        }
    }



    @Transactional(readOnly = true)
    public ProjectDetailsResponse getProjectDetails(Long projectId, String email){
        Project project = projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));

        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));

        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is no member of the project "));

        User createdByUser = null;
        if(project.getCreatedByUserId() != null){
            createdByUser = userRepository.findById(project.getCreatedByUserId()).orElseThrow(() -> new IllegalStateException("Project creator not found"));
        }

        ProjectDetailsResponse response = new ProjectDetailsResponse();
        response.setProjectId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setStatus(project.getProjectStatus());
        response.setRole(membership.getRole());
        if(createdByUser != null){
            response.setCreatedByUserFirstName(createdByUser.getFirstname());
            response.setCreatedByUserLastName(createdByUser.getLastname());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProjectsViewResponse> getProjectView(String email){

        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));

        List<ProjectMembership> memberships = projectMembershipRepository.findAllByUserId(currentUser.getId());

        if (memberships.isEmpty()){
            return List.of();
        }

        List<Long> projectIds = memberships.stream().map(ProjectMembership::getProjectId).toList();
        List<Project> projects = projectRepository.findByProjectIds(projectIds);
        Map<Long, ProjectRole> roleByProjectId = memberships.stream().collect(Collectors.toMap(ProjectMembership::getProjectId, ProjectMembership::getRole));

        return projects.stream().map(project -> new ProjectsViewResponse(project.getId(), project.getName(), project.getProjectStatus(), roleByProjectId.get(project.getId()))).toList();
    }








    @Transactional
    public CreateLongToDoResponse createLongToDo(Long projectId, CreateLongToDoRequest request, String email){

        if(!request.getEndDateTime().isAfter(request.getStartDateTime())){
            throw new IllegalArgumentException("End date must be after start date");
        }


        Project project = projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));


        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));


        ProjectMembership currentMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("No project membership"));


        if(currentMembership.getRole() != ProjectRole.PROJECT_LEADER){
            throw new SecurityException("No permission to create a longToDo");
        }


        List<Long> assignedMemberIds = request.getAssignedMemberIds() == null
                                                ? List.of()
                                                : request.getAssignedMemberIds();

        Set<Long> uniqueMemberIds = new HashSet<>(assignedMemberIds);
        if(uniqueMemberIds.size() != assignedMemberIds.size()){
            throw new IllegalArgumentException("Duplicate participants Ids ");
        }

        for(Long userId : uniqueMemberIds){
            boolean isProjectMember = projectMembershipRepository.existsByProjectIdAndUserId(projectId, userId);
            if(!isProjectMember){
                throw new IllegalArgumentException("User is not a project member: " + userId);
            }
        }


        LongToDo longToDo = new LongToDo();
        longToDo.setProjectId(projectId);
        longToDo.setTitle(request.getTitle());
        longToDo.setDescription(request.getDescription());
        longToDo.setStartTime(request.getStartDateTime());
        longToDo.setEndTime(request.getEndDateTime());
        longToDo.setPriority(request.getPriority());
        longToDo.setDone(false);


        LongToDo savedLongToDo = longToDoRepository.save(longToDo);
        Long longToDoId = savedLongToDo.getToDoId();


        for(Long userId : uniqueMemberIds){
            ToDoParticipants participant= new ToDoParticipants();
            participant.setToDoId(longToDoId);
            participant.setUserId(userId);
            toDoParticipantsRepository.save(participant);
        }


        List<CreateSubtaskRequest> subtaskRequests = request.getSubtasks() == null
                                        ? List.of()
                                        : request.getSubtasks();

        for(CreateSubtaskRequest subtaskRequest : subtaskRequests){



            Subtask subtask = new Subtask();
            subtask.setTitle(subtaskRequest.getTitle());
            subtask.setDescription(subtaskRequest.getDescription());
            subtask.setPriority(subtaskRequest.getPriority());
            subtask.setLongToDoId(longToDoId);
            subtask.setDone(false);
            subtask.setAssigneeId(null);
            subtaskRepository.save(subtask);
        }

        return new CreateLongToDoResponse("long todo created", longToDoId);



    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Long projectId, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        boolean currentUserIsMember = projectMembershipRepository.existsByProjectIdAndUserId(projectId, currentUser.getId());
        if(!currentUserIsMember){
            throw new IllegalStateException("Project not found");
        }

        List<ProjectMembership> memberships = projectMembershipRepository.findAllByProjectId(projectId);

        return memberships.stream().map(membership -> {
            User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> new IllegalStateException("User not found"));

            ProjectMemberResponse response = new ProjectMemberResponse(user.getId(), user.getFirstname(), user.getLastname(), membership.getRole());
            response.setEmail(user.getEmail());
            return response;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<LongToDosResponse> getLongToDos(Long projectId, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("Project not found"));

        List<LongToDo> longToDos = longToDoRepository.findAllByProjectId(projectId);

        if(membership.getRole() == ProjectRole.MEMBER){
            longToDos = longToDos.stream().filter(longToDo -> toDoParticipantsRepository.existsByLongTodoIdAndUserId(longToDo.getToDoId(), currentUser.getId()))
                        .toList();
        }

        return longToDos.stream().map(this::toLongToDosResponse).toList();
    }

    private LongToDosResponse toLongToDosResponse(LongToDo longToDo){
        List<Long> assignedMemberIds = toDoParticipantsRepository.findAllByLongToDoId(longToDo.getToDoId()).stream().map(ToDoParticipants::getUserId).toList();
        List<SubtaskResponse> subtasks = subtaskRepository.findByLongToDoId(longToDo.getToDoId()).stream().map(this::toSubtaskResponse).toList();
        return new LongToDosResponse(longToDo.getToDoId(), longToDo.getTitle(), longToDo.getDescription(), longToDo.getPriority(), longToDo.getStartTime(), longToDo.getEndTime(), longToDo.isDone(), subtasks, assignedMemberIds);
    }

    private SubtaskResponse toSubtaskResponse(Subtask subtask){
        return new SubtaskResponse(subtask.getTitle(), subtask.getDescription(), subtask.getSubtaskId(), subtask.isDone(), subtask.getAssigneeId(), subtask.getPriority());
    }

    @Transactional
    public LongToDosResponse setSubtaskDone(Long longToDoId, Long subtaskId, UpdateSubtaskDoneRequest request, String email){
        if(request == null || request.getUpdate() == null || !"done".equals(request.getUpdate().getAttribute()) || request.getUpdate().getNewValue() == null){
            throw new IllegalArgumentException("Expected update.attribut = 'done' and a boolean newValue");
        }
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Long todo not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        Subtask subtask = subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId).orElseThrow(() -> new IllegalStateException("Subtask does not belong to this long todo"));


        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        boolean isAssigneeUser = subtask.getAssigneeId() != null && subtask.getAssigneeId().equals(currentUser.getId());
        if(!isProjectLeader && !isAssigneeUser){
            throw new IllegalStateException("User is not allowed to update this subtask");
        }
        boolean newDoneValue = request.getUpdate().getNewValue();
        subtask.setDone(newDoneValue);
        subtaskRepository.save(subtask);
        List<Subtask> allSubtasks = subtaskRepository.findByLongToDoId(longToDoId);
        boolean longToDoDone = !allSubtasks.isEmpty() && allSubtasks.stream().allMatch(Subtask::isDone);
        longToDo.setDone(longToDoDone);
        longToDoRepository.save(longToDo);
        LongToDo updatedLongToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Updated long todo not found"));
        return toLongToDosResponse(updatedLongToDo);
    }

    @Transactional
    public LongToDosResponse setLongToDoDone(Long longToDoId, Boolean done, String email){
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
        return toLongToDosResponse(updatedLongToDo);
    }

    @Transactional
    public Subtask claimSubtask(Long longToDoId, Long subtaskId, ClaimSubtaskRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId)
                .orElseThrow(() -> new IllegalStateException("Long todo not found"));

        if (!projectMembershipRepository.existsByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId())) {
            throw new SecurityException("User is not a project member");
        }

        Subtask subtask = subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId)
                .orElseThrow(() -> new IllegalStateException("Subtask does not belong to this long todo"));
        if (subtask.getAssigneeId() != null
                || subtaskRepository.claimIfUnassigned(subtaskId, currentUser.getId()) != 1) {
            Long assigneeId = subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId)
                    .map(Subtask::getAssigneeId)
                    .orElse(null);
            throw new SubtaskAlreadyAssignedException(assigneeId);
        }

        return subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId)
                .orElseThrow(() -> new IllegalStateException("Subtask not found"));
    }

    public static class SubtaskAlreadyAssignedException extends RuntimeException {
        private final Long assigneeId;

        public SubtaskAlreadyAssignedException(Long assigneeId) {
            this.assigneeId = assigneeId;
        }

        public Long getAssigneeId() {
            return assigneeId;
        }
    }

    @Transactional
    public String assignSubtask(Long longToDoId, Long subtaskId, AssignSubtaskRequest request, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId)
                .orElseThrow(() -> new IllegalStateException("Long todo not found"));
        if (!projectMembershipRepository.existsByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId())) {
            throw new SecurityException("User is not a project member");
        }
        subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId)
                .orElseThrow(() -> new IllegalStateException("Subtask does not belong to this long todo"));

        if (request == null || request.getFrom() == null || request.getTo() == null) {
            throw new IllegalArgumentException("Fields 'from' and 'to' are required");
        }
        if (request.getAssigned() == null) {
            if (subtaskRepository.releaseIfAssignedTo(subtaskId, currentUser.getId()) != 1) {
                throw new IllegalStateException("Subtask is not assigned to the current user");
            }
            return "subtask is now free";
        }
        if (!currentUser.getId().equals(request.getAssigned())) {
            throw new SecurityException("A subtask can only be assigned to the current user");
        }
        if (subtaskRepository.claimIfUnassigned(subtaskId, currentUser.getId()) != 1) {
            throw new SubtaskAlreadyAssignedException(
                    subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId)
                            .map(Subtask::getAssigneeId)
                            .orElse(null));
        }
        return "subtask assigned";
    }

    @Transactional
    public MessageResponse deleteSubtask(Long longToDoId, Long subtaskId, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Long todo not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));

        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        if(!isProjectLeader){
            throw new IllegalStateException("Only a project leader can delete a subtask");
        }
        subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId).orElseThrow(() -> new IllegalStateException("Subtask does not belong to this long todo"));
        subtaskRepository.deleteById(subtaskId);
        return new MessageResponse("subtask deleted");
    }

    @Transactional
    public MessageResponse deleteLongToDo(Long longToDoId, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Long todo not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));

        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        if(!isProjectLeader){
            throw new IllegalStateException("Only a project leader can delete a long todo");
        }
        subtaskRepository.deleteAllByLongToDoId(longToDoId);
        toDoParticipantsRepository.deleteAllByLongTodoId(longToDoId);
        longToDoRepository.deleteById(longToDoId);
        return new MessageResponse("long todo deleted");
    }

    @Transactional
    public MessageResponse deleteMember(Long projectId, Long targetUserId, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        ProjectMembership targetMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, targetUserId).orElseThrow(() -> new IllegalStateException("User to delete is not a project member"));
        boolean isSelf = currentUser.getId().equals(targetUserId);
        if(isSelf){
            return leaveProject(projectId, targetMembership);
        }
        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        if(!isProjectLeader){
            throw new IllegalStateException("Only a project leader can remove another member");
        }
        return removeMember(projectId, targetUserId, targetMembership);
    }

    private MessageResponse leaveProject(Long projectId, ProjectMembership membership){
        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        if(isProjectLeader){
            List<ProjectMembership> memberships = projectMembershipRepository.findAllByProjectId(projectId);
            long remainingProjectLeaders = memberships.stream().filter(otherMembership -> otherMembership.getRole() == ProjectRole.PROJECT_LEADER).filter(otherMembership -> !otherMembership.getUserId().equals(membership.getUserId())).count();
            if(remainingProjectLeaders == 0){
                throw new IllegalStateException("A project must have at least one leader.");
            }
        }
        removeUserFromProjectData(projectId, membership.getUserId());
        projectMembershipRepository.deleteByProjectIdAndUSerId(projectId, membership.getUserId());
        return new MessageResponse("You have left the project");
    }

    private MessageResponse removeMember(Long projectId, Long targetUserId, ProjectMembership targetMembership){
        if(targetMembership.getRole() == ProjectRole.PROJECT_LEADER){
            throw new IllegalStateException("Only members can be removed by a project leader");
        }
        removeUserFromProjectData(projectId, targetUserId);
        projectMembershipRepository.deleteByProjectIdAndUSerId(projectId, targetUserId);
        return new MessageResponse("Member removed from the project");
    }

    private void removeUserFromProjectData(Long projectId, Long userId){
        List<LongToDo> longToDos = longToDoRepository.findAllByProjectId(projectId);


        for(LongToDo longToDo : longToDos){
            Long longToDoId = longToDo.getToDoId();
            List<Subtask> assignedSubtasks = subtaskRepository.findByLongToDoId(longToDo.getToDoId());
            for(Subtask subtask : assignedSubtasks){
                if(userId.equals(subtask.getAssigneeId())){
                    subtask.setAssigneeId(null);
                    subtaskRepository.save(subtask);
                }
            }


            toDoParticipantsRepository.deleteByLongTodoIdAndUserId(longToDoId, userId);
        }


        List<Meeting> meetings = meetingRepository.findAllByProjectId(projectId);
        for(Meeting meeting : meetings){
            meetingParticipantsRepository.deleteByMeetingIdAndUserId(meeting.getMeetingId(), userId);
        }
    }

    @Transactional
    public MessageResponse deleteProject(Long projectId, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));
        ProjectMembership currentMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        if(currentMembership.getRole() != ProjectRole.PROJECT_LEADER){
            throw new IllegalStateException("Only a project leader can delete a project");
        }
        List<LongToDo> longToDos = longToDoRepository.findAllByProjectId(projectId);
        List<Meeting> meetings = meetingRepository.findAllByProjectId(projectId);
        List<ProjectMembership> memberships = projectMembershipRepository.findAllByProjectId(projectId);

        for(LongToDo longToDo : longToDos){
            Long longToDoId = longToDo.getToDoId();
            subtaskRepository.deleteAllByLongToDoId(longToDoId);
            for(ProjectMembership membership : memberships){

                toDoParticipantsRepository.deleteByLongTodoIdAndUserId(longToDoId, membership.getUserId());
            }


        }
        for(Meeting meeting : meetings){
            Long meetingId = meeting.getMeetingId();
            for(ProjectMembership membership : memberships){

                meetingParticipantsRepository.deleteByMeetingIdAndUserId(meetingId, membership.getUserId());
            }
        }


        meetingRepository.deleteByProjectId(projectId);
        longToDoRepository.deleteByProjectId(projectId);
        projectMembershipRepository.deleteByProjectId(projectId);
        projectRepository.deleteByProjectId(projectId);
        return new MessageResponse("Project deleted");
    }

    @Transactional
    public MessageResponse updateProjectMemberRole(Long projectId, Long userId, UpdateProjectMemberRoleRequest request, String requesterEmail) {
        Project project = projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));
        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new IllegalStateException("Requester not found"));
        ProjectMembership requesterMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, requester.getId()).orElseThrow(() -> new IllegalStateException("Requester is not a project member"));

        if (requesterMembership.getRole() != ProjectRole.PROJECT_LEADER) {
            throw new IllegalStateException("Only a project leader can update member roles");
        }

        ProjectMembership targetMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, userId).orElseThrow(() -> new IllegalStateException("Target user is not a project member"));

        if (targetMembership.getRole() == ProjectRole.PROJECT_LEADER && !requester.getId().equals(userId)) {
            throw new IllegalStateException("Cannot change the role of another project leader");
        }

        if(targetMembership.getRole() == ProjectRole.PROJECT_LEADER && request.role() != ProjectRole.PROJECT_LEADER && !requester.getId().equals(userId)) {
            List<ProjectMembership> projectLeaders = projectMembershipRepository.findByProjectIdAndRole(projectId, ProjectRole.PROJECT_LEADER);
            boolean anotherProjectLeaderExists = projectLeaders.stream().anyMatch(membership -> !membership.getUserId().equals(requester.getId()));
            if (!anotherProjectLeaderExists) {
                throw new IllegalStateException("project needs at least one project leader.");
            }
        }

        targetMembership.setRole(request.role());
        projectMembershipRepository.save(targetMembership);

        return new MessageResponse("role updated");
    }

    @Transactional
    public MessageResponse updateLongToDo(Long longToDoId, UpdateLongToDoRequest request, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Long todo not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        if(!isProjectLeader){
            throw new IllegalStateException("Only a project leader can update a long todo");
        }
        if(!request.getStartDateTime().isBefore(request.getEndDateTime())){
            throw new IllegalArgumentException("Start date and time must be before end date time");
        }
        List<Long> assignedMemberIds = request.getAssignedMemberIds().stream().distinct().toList();
        for(Long userId : assignedMemberIds){
            boolean isMember = projectMembershipRepository.existsByProjectIdAndUserId(longToDoId, userId);
            if(!isMember){
                throw new IllegalArgumentException("User is not a project member:" + userId);
            }
        }
        List<Subtask> existingSubtasks = subtaskRepository.findByLongToDoId(longToDoId);
        Map<Long, Subtask> existingSubtasksbyId = existingSubtasks.stream().collect(Collectors.toMap(Subtask::getSubtaskId, subtask -> subtask));
        List<UpdateSubtaskRequest> requestedSubtasks = request.getSubtasks();
        Set<Long> requestedExistingSubtaskIds = new HashSet<>();
        for(UpdateSubtaskRequest request2 : requestedSubtasks){
            Long subtaskId = request2.getSubtaskId();
            if(subtaskId == null){
                continue;
            }
            if(!existingSubtasksbyId.containsKey(subtaskId)){
                throw new IllegalArgumentException("Subtask does not belong to this long todo: " + subtaskId);
            }
            if(!requestedExistingSubtaskIds.add(subtaskId)){
                throw new IllegalArgumentException("SubtaskId was submitted more than once: " + subtaskId);
            }
        }

        longToDo.setTitle(request.getTitle());
        longToDo.setDescription(request.getDescription());
        longToDo.setStartTime(request.getStartDateTime());
        longToDo.setEndTime(request.getEndDateTime());
        longToDo.setPriority(request.getPriority());
        longToDoRepository.save(longToDo);

        toDoParticipantsRepository.deleteAllByLongTodoId(longToDoId);
        for(Long userId : assignedMemberIds){
            ToDoParticipants participant = new ToDoParticipants();
            participant.setToDoId(longToDoId);
            participant.setUserId(userId);
            toDoParticipantsRepository.save(participant);
        }

        for(Subtask existingSubtask : existingSubtasks){
            Long existingSubtaskId = existingSubtask.getSubtaskId();
            if(!requestedExistingSubtaskIds.contains(existingSubtaskId)){
                subtaskRepository.deleteById(existingSubtaskId);
            }
        }

        for(UpdateSubtaskRequest subtaskRequest : request.getSubtasks()){
            Subtask subtask;
            if(subtaskRequest.getSubtaskId() == null){
                subtask = new Subtask();
                subtask.setLongToDoId(longToDoId);
                subtask.setDone(false);
                subtask.setAssigneeId(null);
            }
            else{
                subtask = existingSubtasksbyId.get(subtaskRequest.getSubtaskId());
            }
            subtask.setTitle(subtaskRequest.getTitle());
            subtask.setDescription(subtaskRequest.getDescription());
            subtask.setPriority(subtaskRequest.getPriority());
            subtaskRepository.save(subtask);
        }

        return new MessageResponse("long todo updated");
    }

    @Transactional
    public MessageResponse updateProject(Long projectId, UpdateProjectRequest request, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        Project project = projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        boolean isProjectLeader = membership.getRole() == ProjectRole.PROJECT_LEADER;
        if(!isProjectLeader){
            throw new IllegalStateException("Only a project leader can update a project");
        }
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setProjectStatus(request.getStatus());
        projectRepository.save(project);
        return new MessageResponse("Project updated");
    }

    @Transactional
    public MessageResponse updateSubtaskTime(Long longToDoId, Long subtaskId, UpdateSubtaskTimeRequest request, String email){
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
        LongToDo longToDo = longToDoRepository.findById(longToDoId).orElseThrow(() -> new IllegalStateException("Long todo not found"));
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(longToDo.getProjectId(), currentUser.getId()).orElseThrow(() -> new IllegalStateException("User is not a project member"));
        Subtask subtask = subtaskRepository.findByIdAndLongToDoId(subtaskId, longToDoId).orElseThrow(() -> new IllegalStateException("Subtask does not belong to this long todo"));

        if(!currentUser.getId().equals(subtask.getAssigneeId())){
            throw new IllegalStateException("You can only set the Time of your assigned subtasks");
        }

        if(!request.from().isBefore(request.to())){
            throw new IllegalArgumentException("Start date must be before end date");
        }

        int updated = subtaskRepository.updateScheduleIfAssignedTo(longToDoId, subtaskId, currentUser.getId(), request.from(), request.to());
        if(updated != 1){
            throw new IllegalStateException("subtask time could not be updated");
        }
        return new MessageResponse("subtask time updated");


    }
}
