package com.example.epager.incident.dto;

import jakarta.validation.constraints.NotNull;

public record AcknowledgeIncidentRequest(@NotNull Long userId) {
}
