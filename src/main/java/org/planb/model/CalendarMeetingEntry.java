//KI Log 67
package org.planb.model;

import java.time.OffsetDateTime; //KI Log 67
public class CalendarMeetingEntry {

    private Long meetingId;
    private String title;
    private String projectName;
    private OffsetDateTime from;
    private OffsetDateTime to; 
    private boolean done; 

    public CalendarMeetingEntry(Long meetingId, String title, String projectName, OffsetDateTime from, OffsetDateTime to, boolean done) {
        this.meetingId = meetingId;
        this.title = title;
        this.projectName = projectName;
        this.from = from;
        this.to = to;
        this.done = done;
    }

    public Long getMeetingId() {
        return meetingId;
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
}