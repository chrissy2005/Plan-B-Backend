package org.planb.dto;

public class CreateLongToDoResponse {
    String message;
    Long longToDoId;

    public CreateLongToDoResponse(){

    }

    public CreateLongToDoResponse(String message, Long longToDoId){
        this.message = message;
        this.longToDoId = longToDoId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLongToDoId() {
        return longToDoId;
    }

    public void setLongToDoId(Long longToDoId) {
        this.longToDoId = longToDoId;
    }

}
