package org.planb.dto;

import java.util.List;

public record GoogleCalendarBusyResponse(List<BusyPeriod> periods) {
    public record BusyPeriod(String from, String to) {
    }
}
