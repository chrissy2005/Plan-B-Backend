package org.planb.model;

public class MeetingParticipants {
    private Long userId;
    private Long meetingId;
    
    public MeetingParticipants(){

    }

    public MeetingParticipants(Long userId, Long meetingId){
        this.userId = userId;
        this.meetingId = meetingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }
    
}