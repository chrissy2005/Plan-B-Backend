package org.planb.service;

import org.planb.dto.ProjectResponse;
import org.planb.dto.AddProjectMemberRequest;
import org.planb.dto.CreateProjectRequest;
import org.planb.dto.ProjectDetailsResponse;
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

@Service 
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectMembershipRepository projectMembershipRepository, UserRepository userRepository, JdbcProjectMembershipRepository jdbcProjectMembershipRepository){
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
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
}
