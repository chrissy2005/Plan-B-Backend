package org.planb.dto;

import org.planb.model.ProjectRole;
import org.planb.model.ProjectStatus;

public class ProjectDetailsResponse {
    private Long projectId;
    private String name;
    private String description;
    private ProjectStatus status;
    private ProjectRole role;
    private String createdByUserLastName;
    private String createdByUserFirstName;

    public ProjectDetailsResponse(){

    }

    public ProjectDetailsResponse(Long projectId, String name, String description, ProjectStatus status, ProjectRole role, String createdByUserLastName, String createdByUserFirstName){
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.role = role;
        this.createdByUserLastName = createdByUserLastName;
        this.createdByUserFirstName = createdByUserFirstName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCreatedByUserLastName() {
        return createdByUserLastName;
    }

    public void setCreatedByUserLastName(String createdByUserLastName) {
        this.createdByUserLastName = createdByUserLastName;
    }

    public String getCreatedByUserFirstName() {
        return createdByUserFirstName;
    }

    public void setCreatedByUserFirstName(String createdByUserFirstName) {
        this.createdByUserFirstName = createdByUserFirstName;
    }

}
