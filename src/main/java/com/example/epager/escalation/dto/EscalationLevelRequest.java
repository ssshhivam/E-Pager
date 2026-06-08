package com.example.epager.escalation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EscalationLevelRequest(
        @NotNull @Min(1) Integer levelNumber,
        @NotNull Long userId,
        @NotNull @Min(1) Integer waitMinutes
) {
}
