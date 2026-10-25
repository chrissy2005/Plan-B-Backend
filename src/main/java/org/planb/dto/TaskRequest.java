package org.planb.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record TaskRequest(
        @NotBlank String type,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        LocalDateTime start,
        @NotNull LocalDateTime end,
        @NotBlank String priority,
        boolean allParticipants,
        List<@NotNull Long> participantIds,
        List<@NotNull @Valid SubtaskInput> subtasks) {
    public record SubtaskInput(Long id, @NotBlank @Size(max = 150) String title,
            @Size(max = 2000) String description, Long assigneeId, boolean done) {}
}
