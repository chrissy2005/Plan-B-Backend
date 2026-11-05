package org.planb.dto;

public class CalendarLongToDoResponse {
    
    private String title;
    private String projectName;
    private Long longToDoId;
    private String from;
    private String to;
    private boolean done;

    public CalendarLongToDoResponse(String title, String projectName, Long longToDoId, String from, String to, boolean done) {
        this.title = title;
        this.projectName = projectName;
        this.longToDoId = longToDoId;
        this.from = from;
        this.to = to;
        this.done = done;
    }

    public String getTitle() {
        return title;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getLongToDoId() {
        return longToDoId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isDone() {
        return done;
    }
}