package org.planb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record CreateMeetingRequest(
        @NotBlank(message = "title missing") @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull(message = "startDateTime missing") LocalDateTime startDateTime,
        @NotNull(message = "endDateTime missing") LocalDateTime endDateTime,
        @NotEmpty(message = "participants missing") List<@NotNull Long> participantIds) {
}
