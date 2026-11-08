package org.planb.dto;

import java.util.List;


public class CalendarResponse {
    
    private String message;
    private String from;
    private String to;
    private List<CalendarMeetingResponse> meetings;
    private List<CalendarSubtaskResponse> subtasks;
    private List<CalendarLongToDoResponse> longToDos;
    

    public CalendarResponse(String message, String from, String to, List<CalendarMeetingResponse> meetings, List<CalendarSubtaskResponse> subtasks, List<CalendarLongToDoResponse> longToDos) {
        
        this.message = message;
        this.from = from;
        this.to = to;
        this.meetings = meetings;
        this.subtasks = subtasks;
        this.longToDos = longToDos;
    }

    

    public String getMessage() {
        return message;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public List<CalendarMeetingResponse> getMeetings() {
        return meetings;
    }

    public List<CalendarSubtaskResponse> getSubtasks() {
        return subtasks;
    }

    public List<CalendarLongToDoResponse> getLongToDos() {
        return longToDos;
    }
}