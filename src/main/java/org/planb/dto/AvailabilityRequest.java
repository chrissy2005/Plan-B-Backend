package org.planb.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(
    @NotEmpty(message= "At least one userId is required")  
    List<@NotNull Long> userIds,

    @NotNull(message= "Start is required") 
    OffsetDateTime start,

    @NotNull(message= "End is required") 
    OffsetDateTime end
) 
{
}
