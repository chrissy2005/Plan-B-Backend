package org.planb.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.planb.model.TaskPriority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateLongToDoRequest {
    @NotBlank(message = "Long To-Do needs a title")
    @Size(max = 150, message = "Title must not exceed 150 characters" )
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Start date and time is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date and time is required")
    private LocalDateTime endDateTime;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    @NotEmpty(message = "At least one member is required")
    private List<@NotNull Long> assignedMemberIds;

    @Valid 
    private List<UpdateSubtaskRequest> subtasks;

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

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public List<Long> getAssignedMemberIds() {
        return assignedMemberIds;
    }

    public void setAssignedMemberIds(List<Long> assignedMemberIds) {
        this.assignedMemberIds = assignedMemberIds;
    }

    public List<UpdateSubtaskRequest> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<UpdateSubtaskRequest> subtasks) {
        this.subtasks = subtasks;
    }

    
}
