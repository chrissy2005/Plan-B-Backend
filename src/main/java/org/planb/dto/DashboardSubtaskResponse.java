package org.planb.dto;

import java.time.LocalDateTime;

import org.planb.model.TaskPriority;

public record DashboardSubtaskResponse(
    Long subtaskId,
    Long longToDoId,
    String longToDoTitle,
    Long projectId,
    String projectName,
    String title,
    String description,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    TaskPriority priority,
    boolean done
) 
{
}
