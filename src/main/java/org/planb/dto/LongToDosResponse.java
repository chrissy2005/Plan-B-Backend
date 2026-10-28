package org.planb.dto;

import java.time.LocalDateTime;

import org.planb.model.TaskPriority;
import java.util.List;

public class LongToDosResponse {
    private Long longToDoId;
    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean done;
    private List<SubtaskResponse> subtasks;
    private List<Long> assignedMemberIds;

    public LongToDosResponse(){}

    public LongToDosResponse(Long longToDoId, String title, String description, TaskPriority priority, LocalDateTime startDateTime, LocalDateTime endDateTime, boolean done, List<SubtaskResponse> subtasks, List<Long> assignedMemberIds) {
        this.longToDoId = longToDoId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.done = done;
        this.subtasks = subtasks;
        this.assignedMemberIds = assignedMemberIds;
    }

    public Long getLongToDoId() {
        return longToDoId;
    }

    public void setLongToDoId(Long longToDoId) {
        this.longToDoId = longToDoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
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

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SubtaskResponse> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<SubtaskResponse> subtasks) {
        this.subtasks = subtasks;
    }

    public List<Long> getAssignedMemberIds() {
        return assignedMemberIds;
    }

    public void setAssignedMemberIds(List<Long> assignedMemberIds) {
        this.assignedMemberIds = assignedMemberIds;
    }

}