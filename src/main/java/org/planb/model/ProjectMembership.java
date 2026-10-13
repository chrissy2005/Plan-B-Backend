package org.planb.model;

public class ProjectMembership {
    private Long projectId;
    private Long userId;
    private ProjectRole role;

    public ProjectMembership(){

    }

     public ProjectMembership(Long projectId, Long userId, ProjectRole role){
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

     public Long getProjectId() {
         return projectId;
     }

     public void setProjectId(Long projectId) {
         this.projectId = projectId;
     }

     public Long getUserId() {
         return userId;
     }

     public void setUserId(Long userId) {
         this.userId = userId;
     }

     public ProjectRole getRole() {
         return role;
     }

     public void setRole(ProjectRole role) {
         this.role = role;
     }
    
}
