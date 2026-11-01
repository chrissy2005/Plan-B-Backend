package org.planb.service;

import org.planb.dto.ProjectResponse;
import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.CreateLongToDoRequest;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.CreateSubtaskRequest;
import org.planb.dto.LongToDosResponse;
import org.planb.dto.ProjectDetailsResponse;
import org.planb.dto.ProjectMemberResponse;
import org.planb.dto.ProjectsViewResponse;
import org.planb.dto.SubtaskResponse;
import org.planb.dto.UpdateSubtaskDoneRequest;
import org.planb.dto.CreateLongToDoResponse;
import org.planb.dto.ClaimSubtaskRequest;
import org.planb.model.LongToDo;
import org.planb.model.Project;
import org.planb.model.ProjectMembership;
import org.planb.model.ProjectRole;
import org.planb.model.ProjectStatus;
import org.planb.model.Subtask;
import org.planb.model.ToDoParticipants;
import org.planb.model.User;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.function.Function;
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

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMembershipRepository projectMembershipRepository,
                          UserRepository userRepository,
                          JdbcProjectMembershipRepository jdbcProjectMembershipRepository,
                          JdbcTemplate jdbcTemplate,
                          LongToDoRepository longToDoRepository,
                          ToDoParticipantsRepository toDoParticipantsRepository,
                          SubtaskRepository subtaskRepository){
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.longToDoRepository = longToDoRepository;
        this.toDoParticipantsRepository = toDoParticipantsRepository;
        this.subtaskRepository = subtaskRepository;
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

    /* this was the old AI-Method from  22.10 
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProjectMembers(Long projectId, String email) {
        User currentUser = requireProjectMember(projectId, email);
        return projectMembershipRepository.findAllByProjectId(projectId).stream()
                .map(membership -> {
                    User user = userRepository.findById(membership.getUserId())
                            .orElseThrow(() -> new IllegalStateException("Project member not found"));
                    Map<String, Object> member = new HashMap<>();
                    member.put("userId", user.getId());
                    member.put("firstName", user.getFirstname());
                    member.put("lastName", user.getLastname());
                    member.put("name", (user.getFirstname() + " " + user.getLastname()).trim());
                    member.put("email", user.getEmail());
                    member.put("role", membership.getRole());
                    return member;
                })
                .toList();
    }
    */

    /* this was the old AI-Method from 22.10 
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProjectMeetings(Long projectId, String email) {
        requireProjectMember(projectId, email);
        List<Map<String, Object>> meetings = jdbcTemplate.queryForList("""
                SELECT meeting_id, project_id, title, description, starts_at, ends_at, is_completed
                FROM meetings
                WHERE project_id = ?
                ORDER BY starts_at, meeting_id
                """, projectId);
        meetings.forEach(meeting -> {
            Long meetingId = ((Number) meeting.get("meeting_id")).longValue();
            meeting.put("meetingId", meetingId);
            meeting.put("projectId", projectId);
            meeting.put("startDateTime", meeting.remove("starts_at"));
            meeting.put("endDateTime", meeting.remove("ends_at"));
            meeting.put("done", meeting.remove("is_completed"));
            meeting.put("priority", "MEDIUM");
            meeting.put("participantIds", jdbcTemplate.queryForList(
                    "SELECT user_id FROM meeting_participants WHERE meeting_id = ? ORDER BY user_id",
                    Long.class, meetingId));
        });
        return meetings;
    }
    */

    /* this was the old AI-Method from 22.10
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProjectLongToDos(Long projectId, String email) {
        requireProjectMember(projectId, email);
        List<Map<String, Object>> todos = jdbcTemplate.queryForList("""
                SELECT todo_id, project_id, title, description, starts_at, ends_at, is_completed, priority
                FROM long_todos
                WHERE project_id = ?
                ORDER BY starts_at, todo_id
                """, projectId);
        todos.forEach(todo -> {
            Long todoId = ((Number) todo.get("todo_id")).longValue();
            todo.put("longToDoId", todoId);
            todo.put("projectId", projectId);
            todo.put("startDateTime", todo.remove("starts_at"));
            todo.put("endDateTime", todo.remove("ends_at"));
            todo.put("done", todo.remove("is_completed"));
            todo.put("assignedMemberIds", jdbcTemplate.queryForList(
                    "SELECT user_id FROM todo_participants WHERE todo_id = ? ORDER BY user_id",
                    Long.class, todoId));
            List<Map<String, Object>> subtasks = jdbcTemplate.queryForList("""
                    SELECT subtask_id, title, description, is_completed, assignee_id, priority
                    FROM subtasks
                    WHERE todo_id = ?
                    ORDER BY subtask_id
                    """, todoId);
            subtasks.forEach(subtask -> {
                subtask.put("subtaskId", subtask.remove("subtask_id"));
                subtask.put("done", subtask.remove("is_completed"));
                subtask.put("assigneeId", subtask.remove("assignee_id"));
            });
            todo.put("subtasks", subtasks);
        });
        return todos;
    }
    */
   /* old ai-method (Hilfsmethode) 
    private User requireProjectMember(Long projectId, String email) {
        projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new SecurityException("no permission"));
        return currentUser;
    }
    */

    @Transactional 
    public CreateLongToDoResponse createLongToDo(Long projectId, CreateLongToDoRequest request, String email){
        //check input times
        if(!request.getEndDateTime().isAfter(request.getStartDateTime())){ 
            throw new IllegalArgumentException("End date must be after start date");
        }

        //check if projectId exists
        Project project = projectRepository.findByProjectId(projectId).orElseThrow(() -> new IllegalStateException("Project not found"));

        //load current user from JWT-token
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));

        //load project membership for the current user and requested project
        ProjectMembership currentMembership = projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).orElseThrow(() -> new IllegalStateException("No project membership"));
    
        //check if current user is PROJECT_LEADER
        if(currentMembership.getRole() != ProjectRole.PROJECT_LEADER){
            throw new SecurityException("No permission to create a longToDo");
        }

        //check AssignedMemberIds
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

        //create Long-Todo
        LongToDo longToDo = new LongToDo();
        longToDo.setProjectId(projectId);
        longToDo.setTitle(request.getTitle());
        longToDo.setDescription(request.getDescription());
        longToDo.setStartTime(request.getStartDateTime());
        longToDo.setEndTime(request.getEndDateTime());
        longToDo.setPriority(request.getPriority());
        longToDo.setDone(false);

        //save Long-Todo and get todo_id
        LongToDo savedLongToDo = longToDoRepository.save(longToDo);
        Long longToDoId = savedLongToDo.getToDoId();

        //save participants of the Long-Todo
        for(Long userId : uniqueMemberIds){
            ToDoParticipants participant= new ToDoParticipants();
            participant.setToDoId(longToDoId);
            participant.setUserId(userId);
            toDoParticipantsRepository.save(participant);
        }

        //save subtasks from long-todo
        List<CreateSubtaskRequest> subtaskRequests = request.getSubtasks() == null
                                        ? List.of()
                                        : request.getSubtasks();
        
        for(CreateSubtaskRequest subtaskRequest : subtaskRequests){
            
            /* Optional Code: When the projectleader could assign AssigneeId of each subtask 

            if(subtaskRequest.getAssigneeId() != null){
                boolean assigneeIdIsMember = projectMembershipRepository.existsByProjectIdAndUserId(projectId, subtaskRequest.getAssigneeId());
                if(!assigneeIdIsMember){
                    throw new IllegalArgumentException("AssigneeId is not a project member ");
                }
            }
            */

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

        //check if requester is PROJECT_LEADER or MEMBER
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
}
