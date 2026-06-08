package com.example.epager.escalation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EscalationPolicyRequest(
        @NotBlank String projectKey,
        @NotBlank String groupKey,
        @NotBlank String serviceName,
        Boolean enabled,
        @NotEmpty List<@Valid EscalationLevelRequest> levels
) {
}
