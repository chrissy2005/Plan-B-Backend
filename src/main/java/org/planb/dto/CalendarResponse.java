package org.planb.dto;
// TODO: calendarStatus and busyTimes pending decision on Google integration
import java.util.List;
//import org.planb.model.CalendarStatus;

public class CalendarResponse {
    //private CalendarStatus calendarStatus;
    private String message;
    private String from;
    private String to;
    private List<CalendarMeetingResponse> meetings;
    private List<CalendarSubtaskResponse> subtasks;
    private List<CalendarLongToDoResponse> longToDos;
    //private List<BusyTimeResponse> busyTimes;

    public CalendarResponse(/*CalendarStatus calendarStatus, */String message, String from, String to, List<CalendarMeetingResponse> meetings, List<CalendarSubtaskResponse> subtasks, List<CalendarLongToDoResponse> longToDos) {
        //this.calendarStatus = calendarStatus;
        this.message = message;
        this.from = from;
        this.to = to;
        this.meetings = meetings;
        this.subtasks = subtasks;
        this.longToDos = longToDos;
    }

    /*public CalendarStatus getCalendarStatus() {
        return calendarStatus;
    }*/

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