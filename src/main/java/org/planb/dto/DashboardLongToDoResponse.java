package org.planb.dto;

import java.time.LocalDateTime;
import org.planb.model.TaskPriority;

public record DashboardLongToDoResponse(
    Long longToDoId,
    Long projectId,
    String projectName,
    String title,
    String description,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    TaskPriority priority,
    boolean done
) {
}