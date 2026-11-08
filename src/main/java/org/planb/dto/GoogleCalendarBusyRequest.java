package org.planb.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleCalendarBusyRequest {
    @NotBlank
    private String timeMin;

    @NotBlank
    private String timeMax;

    public String getTimeMin() {
        return timeMin;
    }

    public void setTimeMin(String timeMin) {
        this.timeMin = timeMin;
    }

    public String getTimeMax() {
        return timeMax;
    }

    public void setTimeMax(String timeMax) {
        this.timeMax = timeMax;
    }
}
