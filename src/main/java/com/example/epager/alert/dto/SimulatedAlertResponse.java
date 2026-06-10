package com.example.epager.alert.dto;

import com.example.epager.incident.dto.IncidentResponse;
import com.fasterxml.jackson.databind.JsonNode;

public record SimulatedAlertResponse(
        String source,
        String severity,
        String simulatedExternalPostTarget,
        JsonNode payload,
        IncidentResponse incident
) {
}
