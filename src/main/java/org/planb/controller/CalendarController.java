package org.planb.controller;

import org.planb.dto.CalendarResponse;
import org.planb.service.CalendarService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;//KI Log 79
import org.springframework.format.annotation.DateTimeFormat; //KI Log 79
import java.time.LocalDate; //KI Log 79

@RestController
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getCalendarWeek (@RequestParam(name = "week", required = false) 
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
    LocalDate week, Authentication authentication) { //KI Log 76
        //KI Log 77
        String email = authentication.getName();
        CalendarResponse response = calendarService.getCalendarWeek(email, week);
        return ResponseEntity.ok(response);
    }
}
