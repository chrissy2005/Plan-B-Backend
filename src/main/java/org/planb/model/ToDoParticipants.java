package org.planb.model;

public class ToDoParticipants {
    private Long userId;
    private Long toDoId;
    
    public ToDoParticipants(){

    }

    public ToDoParticipants(Long userId, Long toDoId){
        this.userId = userId;
        this.toDoId = toDoId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getToDoId() {
        return toDoId;
    }

    public void setToDoId(Long toDoId) {
        this.toDoId = toDoId;
    }
    
}
