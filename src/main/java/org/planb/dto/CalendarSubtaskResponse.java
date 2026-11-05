package org.planb.dto;

public class CalendarSubtaskResponse {
    private String title;
    private String projectName;
    private Long subtaskId;
    private String longToDoTitle;
    private String from;
    private String to;
    private boolean done;

    public CalendarSubtaskResponse(String title, String projectName, Long subtaskId, String longToDoTitle, String from, String to, boolean done) {
        this.title = title;
        this.projectName = projectName;
        this.subtaskId = subtaskId;
        this.longToDoTitle = longToDoTitle;
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

    public Long getSubtaskId() {
        return subtaskId;
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

    public String getLongToDoTitle() {
        return longToDoTitle;
    }
}