package com.example.epager.escalation.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EscalationLevelRequest(
        @NotNull @Min(1) Integer levelNumber,
        @NotNull List<Long> userIdList,
        @NotNull @Min(1) Integer waitMinutes
) {
}
