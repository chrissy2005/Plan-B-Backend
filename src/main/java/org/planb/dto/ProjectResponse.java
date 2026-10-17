package org.planb.dto;

public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long createdByUserId;
    private String message;
    
    public ProjectResponse(){

    }

    public ProjectResponse(String message, Long projectId, String projectName, String projectDescription, Long createdByUserId){
        this.message = message;
        this.id = projectId;
        this.name = projectName;
        this.description = projectDescription;
        this.createdByUserId = createdByUserId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public String getMessage(){
        return message;
    }

}
