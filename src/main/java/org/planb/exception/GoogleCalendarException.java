package org.planb.exception;

public class GoogleCalendarException extends RuntimeException {
    private final int status;

    public GoogleCalendarException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
