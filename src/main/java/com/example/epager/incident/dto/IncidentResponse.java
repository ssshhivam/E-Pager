package com.example.epager.incident.dto;

import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentStatus;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        String source,
        String externalAlertId,
        String serviceName,
        String severity,
        String title,
        String description,
        IncidentStatus status,
        Integer currentEscalationLevel,
        Long assignedUserId,
        String assignedUserName,
        LocalDateTime nextEscalationAt,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getSource(),
                incident.getExternalAlertId(),
                incident.getServiceName(),
                incident.getSeverity(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getCurrentEscalationLevel(),
                incident.getAssignedUser() == null ? null : incident.getAssignedUser().getId(),
                incident.getAssignedUser() == null ? null : incident.getAssignedUser().getName(),
                incident.getNextEscalationAt(),
                incident.getCreatedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt()
        );
    }
}
