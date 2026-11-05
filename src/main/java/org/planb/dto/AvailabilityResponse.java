package org.planb.dto;

import java.util.List;

public record AvailabilityResponse(
    boolean available,
    List<UnavailableMemberResponse> unavailableMembers
) 
{
}
