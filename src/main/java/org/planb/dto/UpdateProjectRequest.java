package org.planb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.planb.model.ProjectStatus;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        ProjectStatus status) {}
