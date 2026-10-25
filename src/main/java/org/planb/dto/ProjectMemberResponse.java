package org.planb.dto;

import org.planb.model.ProjectRole;

public class ProjectMemberResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private ProjectRole role;

    public ProjectMemberResponse(){

    }
    public ProjectMemberResponse(Long userId, String firstName, String lastName, ProjectRole role){
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public ProjectRole getRole() {
        return role;
    }
    public void setRole(ProjectRole role) {
        this.role = role;
    }

    
}
