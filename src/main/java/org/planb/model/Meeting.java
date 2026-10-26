package org.planb.model;

import java.util.List;
import java.time.LocalDateTime;

public class Meeting {
    private Long meetingId;
    private Long projectId;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean done = false;
    private List <Long> participantIds;

    public Meeting() {
    }

    public Meeting(Long meetingId, Long projectId, String title, String description, LocalDateTime startDateTime, LocalDateTime endDateTime, boolean done) {
        this.meetingId = meetingId;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.done = done;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
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
    
    public void setStartDateTime (LocalDateTime startDateTime){
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime (){
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime){
        this.endDateTime = endDateTime;
    }

    public boolean isDone(){
        return done;
    }

    public void setDone (boolean done){
        this.done = done;
    }

    public List<Long> getParticipantIds (){
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds){
        this.participantIds = participantIds;
    }

}