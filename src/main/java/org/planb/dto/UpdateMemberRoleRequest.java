package org.planb.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequest(@NotBlank String role) {}
