package org.planb.dto;

public class CreateMeetingResponse {
    
    private Long meetingId;
    private String message;
    
    public CreateMeetingResponse(){

    }

    public CreateMeetingResponse(String message, Long meetingId){
        this.meetingId = meetingId;
        this.message = message;
    }

    public void setMeetingId (Long meetingId){
        this.meetingId = meetingId;
    }

    public Long getMeetingId (){
        return meetingId;
    }

    public void setMessage (String message){
        this.message = message;
    }

    public String getMessage (){
        return message;
    }

}