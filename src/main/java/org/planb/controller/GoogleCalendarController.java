package org.planb.controller;

import jakarta.validation.Valid;
import org.planb.dto.GoogleCalendarBusyRequest;
import org.planb.dto.GoogleCalendarBusyResponse;
import org.planb.service.GoogleCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleCalendarController {
    private final GoogleCalendarService service;

    public GoogleCalendarController(GoogleCalendarService service) {
        this.service = service;
    }

    @PostMapping("/integrations/google-calendar/busy")
    public ResponseEntity<GoogleCalendarBusyResponse> busy(
            @Valid @RequestBody GoogleCalendarBusyRequest request,
            @RequestHeader(name = "X-Google-Access-Token", required = false)
            String googleAccessToken
    ) {
        return ResponseEntity.ok(service.getBusyPeriods(request, googleAccessToken));
    }
}
