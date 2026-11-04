package org.planb.controller;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.planb.service.DashboardService;
import org.planb.dto.DashboardMeetingResponse;
import org.planb.dto.DashboardLongToDoResponse;
import org.planb.dto.DashboardSubtaskResponse;
import org.planb.model.TaskPriority;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/me")
public class DashboardController {
    
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/meetings")
    public ResponseEntity<List<DashboardMeetingResponse>> getMyMeetings(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                                       @RequestParam(required = false) Boolean done,
                                                                       Authentication authentication) {
        String email = authentication.getName();
        List<DashboardMeetingResponse> meetings = dashboardService.getMyMeetings(email, from, to, done);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/long-todos")
    public ResponseEntity<List<DashboardLongToDoResponse>> getMyLongToDos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                                          @RequestParam(required = false) TaskPriority priority,
                                                                          @RequestParam(required = false) Boolean done,
                                                                          Authentication authentication) {
        String email = authentication.getName();
        List<DashboardLongToDoResponse> longToDos = dashboardService.getMyLongToDos(email, from, to, priority, done);
        return ResponseEntity.ok(longToDos);
    }

    @GetMapping("/subtasks")
    public ResponseEntity<List<DashboardSubtaskResponse>> getMySubtasks(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                                        @RequestParam(required = false) TaskPriority priority,
                                                                        @RequestParam(required = false) Boolean done,
                                                                        Authentication authentication) {
        String email = authentication.getName();
        List<DashboardSubtaskResponse> subtasks = dashboardService.getMySubtasks(email, priority, done, from, to);
        return ResponseEntity.ok(subtasks);
    }
}
