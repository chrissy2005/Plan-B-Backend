package org.planb.model;

import java.time.LocalDateTime;

public class CalendarLongToDoEntry {

    private Long longToDoId;
    private String title;
    private String projectName;
    private LocalDateTime from;
    private LocalDateTime to; 
    private boolean done; 

    public CalendarLongToDoEntry(Long longToDoId, String title, String projectName, LocalDateTime from, LocalDateTime to, boolean done) {
        this.longToDoId = longToDoId;
        this.title = title;
        this.projectName = projectName;
        this.from = from;
        this.to = to;
        this.done = done;
    }

    public Long getLongToDoId() {
        return longToDoId;
    }
    public String getTitle() {
        return title;
    }
    public String getProjectName() {
        return projectName;
    }
    public LocalDateTime getFrom() {
        return from;
    }
    public LocalDateTime getTo() {
        return to;
    }
    public boolean isDone() {
        return done;
    }
}