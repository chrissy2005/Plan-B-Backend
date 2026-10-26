package org.planb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore; //KI Log 27
import jakarta.validation.constraints.AssertTrue; //KI Log 27
import jakarta.validation.constraints.NotNull; //KI Log 27

import java.time.LocalDateTime; //KI Log 28
import java.util.List; //KI Log 29

public class CreateMeetingRequest {

     
     @NotBlank(message = "title missing")
     @Size(max = 100, message = "Meeting title must be at most 100 characters")
     private String title;

     @NotNull(message = "start missing")
     private LocalDateTime startDateTime; //KI Log 28

     @NotNull(message = "end missing")
     private LocalDateTime endDateTime;

    private List<Long> participantIds; //KI Log 29
    private String description;

     //KI Log 27
    @JsonIgnore
    @AssertTrue(message = "end before start")
    public boolean isEndAfterStart() {
        if (startDateTime == null || endDateTime == null) {
            return true;
        }
        return endDateTime.isAfter(startDateTime);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public List<Long> getParticipantIds(){
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds){
        this.participantIds = participantIds;
    }

}