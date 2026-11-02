package org.planb.dto;

public class CalendarMeetingResponse {
    private String title;
    private String projectName;
    private Long meetingId;
    private String from;
    private String to;
    private boolean done;


    public CalendarMeetingResponse(String title, String projectName, Long meetingId, String from, String to, boolean done) {
        this.title = title;
        this.projectName = projectName;
        this.meetingId = meetingId;
        this.from = from;
        this.to = to;
        this.done = done;
    }

    public String getTitle() {
        return title;
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

    public String getProjectName() {
        return projectName;
    }

    public Long getMeetingId() {
        return meetingId;
    }
}