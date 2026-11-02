package org.planb.dto;

import java.time.LocalDateTime;


public record DashboardMeetingResponse(
    Long meetingId,
    Long projectId,
    String projectName,
    String title,
    String description,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    boolean done
) 
{    
}
