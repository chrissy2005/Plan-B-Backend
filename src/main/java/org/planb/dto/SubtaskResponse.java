package org.planb.dto;

import org.planb.model.TaskPriority;

public class SubtaskResponse {
    private String title;
    private String description;
    private Long subtaskId;
    private boolean done;
    private Long assigneeId;
    private TaskPriority priority;

    public SubtaskResponse(){}

    public SubtaskResponse(String title, String description, Long subtaskId, boolean done, Long assigneeId, TaskPriority priority){
        this.title = title;
        this.description = description;
        this.subtaskId = subtaskId;
        this.done = done;
        this.assigneeId = assigneeId;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSubtaskId() {
        return subtaskId;
    }

    public void setSubtaskId(Long subtaskId) {
        this.subtaskId = subtaskId;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    
}
