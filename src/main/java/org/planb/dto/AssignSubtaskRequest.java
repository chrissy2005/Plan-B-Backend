package org.planb.dto;

public class AssignSubtaskRequest {
    private String from;
    private String to;
    private Long assigned;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Long getAssigned() {
        return assigned;
    }

    public void setAssigned(Long assigned) {
        this.assigned = assigned;
    }
}
