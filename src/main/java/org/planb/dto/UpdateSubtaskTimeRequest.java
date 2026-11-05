package org.planb.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record UpdateSubtaskTimeRequest(
    @NotNull(message = "Start date is required")
    OffsetDateTime from,
    @NotNull(message = "End date is required")
    OffsetDateTime to
) {
}
