package org.planb.service;

import org.planb.dto.ProjectResponse;
import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.ProjectDetailsResponse;
import org.planb.dto.ProjectsViewResponse;
import org.planb.model.Project;
import org.planb.model.ProjectMembership;
import org.planb.model.ProjectRole;
import org.planb.model.ProjectStatus;
import org.planb.model.User;
import org.planb.repository.JdbcProjectMembershipRepository;
import org.planb.repository.ProjectMembershipRepository;
import org.planb.repository.ProjectRepository;
import org.planb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service 
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProjectService(ProjectRepository projectRepository, ProjectMembershipRepository projectMembershipRepository, UserRepository userRepository, JdbcProjectMembershipRepository jdbcProjectMembershipRepository, JdbcTemplate jdbcTemplate){
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
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

        projectMembershipRepository.save(membership);
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

    private User requireProjectMember(Long projectId, String email) {
        projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found"));
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        projectMembershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new SecurityException("no permission"));
        return currentUser;
    }
}
