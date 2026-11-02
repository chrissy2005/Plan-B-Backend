package org.planb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.planb.dto.MessageResponse;
import org.planb.service.MeetingService;
import org.springframework.security.core.Authentication;

@RestController 
@RequestMapping("/meetings")
public class MeetingController {
    private final MeetingService meetingService;
    public MeetingController(MeetingService meetingService){
        this.meetingService = meetingService;
    }

    @DeleteMapping ("/{meetingId}")
    public ResponseEntity<MessageResponse> deleteMeeting(@PathVariable Long meetingId, Authentication authentication) {
        String email = authentication.getName();
        MessageResponse response = meetingService.deleteMeeting(meetingId, email);
        return ResponseEntity.ok(response);
    }   
}
