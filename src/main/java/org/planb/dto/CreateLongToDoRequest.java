package org.planb.dto;

import org.planb.model.TaskPriority;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;                // KI log für Subtaskscreate-Liste
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty; //KI-Log für Überprüfung Long-Liste

public class CreateLongToDoRequest {
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
    private List<Long> assignedMemberIds;

    @Valid 
    private List<CreateSubtaskRequest> subtasks;

    public CreateLongToDoRequest(){}

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
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

    public void setStartDateTime(LocalDateTime startDate) {
        this.startDateTime = startDate;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDate) {
        this.endDateTime = endDate;
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

    public List<CreateSubtaskRequest> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<CreateSubtaskRequest> subtasks) {
        this.subtasks = subtasks;
    }
    
}
