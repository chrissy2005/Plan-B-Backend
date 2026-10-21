package org.planb.model;

import java.time.LocalDateTime; //KI-Log Verweis: "Welche Pakete würdest du importieren, um ein Datum in Java auf die Minute genau darszustellen."

public class LongToDo {
    private Long toDoId;
    private Long projectId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean done = false;

    public LongToDo(){
    }

    public LongToDo( Long toDoId, Long projectId, String title, String description, LocalDateTime startTime, LocalDateTime endTime, boolean completed){
        this.toDoId = toDoId;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.done = completed;
    }

    public Long getToDoId() {
        return toDoId;
    }

    public void setToDoId(Long toDoId) {
        this.toDoId = toDoId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setNamTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean completed) {
        this.done = completed;
    }
    
}
