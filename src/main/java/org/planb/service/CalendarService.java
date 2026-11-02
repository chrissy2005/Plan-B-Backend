package org.planb.service;

import org.planb.model.CalendarMeetingEntry;
import org.planb.model.CalendarLongToDoEntry;
import org.planb.model.User; //KI Log 78
import org.planb.repository.UserRepository; //KI Log 78
import org.planb.repository.LongToDoRepository;
import org.planb.repository.MeetingRepository;
import org.planb.dto.CalendarLongToDoResponse;
import org.planb.dto.CalendarMeetingResponse;
import org.planb.dto.CalendarResponse;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek; //KI Log 71
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters; //KI Log 71
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList; //KI Log 74
import java.time.ZoneId; //KI Log 75
import java.time.format.DateTimeFormatter; //KI Log 75

@Service 
public class CalendarService {

    private final MeetingRepository meetingRepository;
    private final LongToDoRepository longToDoRepository;
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin"); //KI Log 70
    private static final DateTimeFormatter MEETING_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"); //KI Log 73
    private static final DateTimeFormatter TODO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final UserRepository userRepository; //KI Log 78
    
    public CalendarService(MeetingRepository meetingRepository, LongToDoRepository longToDoRepository, UserRepository userRepository) {
        this.meetingRepository = meetingRepository;
        this.longToDoRepository = longToDoRepository;
        this.userRepository = userRepository;
        
    }
    
    public CalendarResponse getCalendarWeek (String email, LocalDate week) {
    //KI Log 78
    User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("Authenticated user could not be found"));
    Long userId = user.getId();

    if (week == null) {
        week = LocalDate.now(BERLIN); //KI Log 70
    }

    LocalDate from = week.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); //KI Log 71
    LocalDate to = from.plusDays(6);
    LocalDate nextMonday = from.plusDays(7);

    //KI Log 72
    //für lange ToDos
    LocalDateTime longToDoPeriodStart = from.atStartOfDay(); 
    LocalDateTime longToDoPeriodEnd = nextMonday.atStartOfDay(); 
    //für Meetings
    OffsetDateTime meetingPeriodStart = from.atStartOfDay(BERLIN).toOffsetDateTime();
    OffsetDateTime meetingPeriodEnd = nextMonday.atStartOfDay(BERLIN).toOffsetDateTime();
    
    //Meetings in liste speichern 
    List<CalendarMeetingEntry> meetings = meetingRepository.findAllByParticipantInPeriod(userId, meetingPeriodStart, meetingPeriodEnd);
    List<CalendarMeetingResponse> meetingResponses = new ArrayList<>(); //KI Log 74
    
    //KI Log 74
    for (CalendarMeetingEntry entry : meetings) { 
        Long meetingId = entry.getMeetingId();
        String title = entry.getTitle();
        String projectName = entry.getProjectName();
        String formattedFrom = entry.getFrom().atZoneSameInstant(BERLIN).format(MEETING_DATE_TIME_FORMATTER);
        String formattedTo = entry.getTo().atZoneSameInstant(BERLIN).format(MEETING_DATE_TIME_FORMATTER);
        boolean done = entry.isDone();
        
        CalendarMeetingResponse meetingResponse = new CalendarMeetingResponse(title, projectName, meetingId, formattedFrom, formattedTo, done);
        meetingResponses.add(meetingResponse);
   }
   
   //To Dos in liste speichern 
   List<CalendarLongToDoEntry> longToDos = longToDoRepository.findAllByParticipantInPeriod(userId, longToDoPeriodStart, longToDoPeriodEnd);
   List<CalendarLongToDoResponse> longToDoResponses = new ArrayList<>(); //KI Log 74
   
   //KI Log 74
   for (CalendarLongToDoEntry entry : longToDos) {
    Long longToDoId = entry.getLongToDoId();
    String title = entry.getTitle();
    String projectName = entry.getProjectName();
    String formattedFrom = entry.getFrom().format(TODO_DATE_FORMATTER);
    String formattedTo = entry.getTo().format(TODO_DATE_FORMATTER);
    boolean done = entry.isDone();

    CalendarLongToDoResponse longToDoResponse = new CalendarLongToDoResponse(title, projectName, longToDoId, formattedFrom, formattedTo, done);
    longToDoResponses.add(longToDoResponse);
   }
   
   String message = "calendar successfully retrieved for user";
   // TODO: Subtasks werden noch nicht geladen, da die Tabelle subtasks noch keine Spalten
   // für das Zeitfenster (Beginn/Ende) besitzt. Bis dahin wird eine leere Liste übergeben,
   // damit das Frontend immer eine Liste und nie null erhält.
   CalendarResponse calendarResponse = new CalendarResponse(message, from.toString(), to.toString(), meetingResponses, List.of(), longToDoResponses);
   return calendarResponse;

}
}