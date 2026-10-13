package org.planb.model;

public class Project {
    private Long id;
    private String name;
    private String description;
    private Long createdByUserId;
    
    public Project(){

    }

    public Project(Long projectId, String projectName, String projectDescription, Long createdByUserId){
        this.id = projectId;
        this.name = projectName;
        this.description = projectDescription;
        this.createdByUserId = createdByUserId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long projectId) {
        this.id = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String projectName) {
        this.name = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String projectDescription) {
        this.description = projectDescription;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
