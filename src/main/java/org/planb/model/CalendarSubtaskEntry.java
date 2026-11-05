package org.planb.model;

import java.time.OffsetDateTime;
public class CalendarSubtaskEntry {

    private Long subtaskId;
    private String title;
    private String projectName;
    private String todoTitle;
    private OffsetDateTime from;
    private OffsetDateTime to; 
    private boolean done; 

    public CalendarSubtaskEntry(Long subtaskId, String title, String projectName, String todoTitle, OffsetDateTime from, OffsetDateTime to, boolean done) {
        this.subtaskId = subtaskId;
        this.title = title;
        this.projectName = projectName;
        this.todoTitle = todoTitle;
        this.from = from;
        this.to = to;
        this.done = done;
    }

    public Long getSubtaskId() {
        return subtaskId;
    }
    public String getTitle() {
        return title;
    }
    public String getProjectName() {
        return projectName;
    }
    public OffsetDateTime getFrom() {
        return from;
    }
    public OffsetDateTime getTo() {
        return to;
    }
    public boolean isDone() {
        return done;
    }
    public String getTodoTitle() {
        return todoTitle;
    }

}