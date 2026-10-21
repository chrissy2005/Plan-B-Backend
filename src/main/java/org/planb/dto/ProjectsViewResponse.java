package org.planb.dto;

import org.planb.model.ProjectRole;
import org.planb.model.ProjectStatus;

public class ProjectsViewResponse {
    private Long projectId;
    private String name;
    private ProjectStatus status;
    private ProjectRole role;

    public ProjectsViewResponse(){

    }

    public ProjectsViewResponse(Long projectId, String name, ProjectStatus status, ProjectRole role){
        this.projectId = projectId;
        this.name = name;
        this.status = status;
        this.role = role;
    }

    public Long getProjectId() {
        return projectId;
    }
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public ProjectStatus getStatus() {
        return status;
    }
    public void setStatus(ProjectStatus status) {
        this.status = status;
    }
    public ProjectRole getRole() {
        return role;
    }
    public void setRole(ProjectRole role) {
        this.role = role;
    }
    
}
