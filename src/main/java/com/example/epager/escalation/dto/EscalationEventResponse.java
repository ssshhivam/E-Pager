package com.example.epager.escalation.dto;

import com.example.epager.escalation.EscalationEvent;

import java.time.LocalDateTime;

public record EscalationEventResponse(
        Long id,
        Long incidentId,
        Integer fromLevel,
        Integer toLevel,
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        String reason,
        LocalDateTime createdAt
) {
    public static EscalationEventResponse from(EscalationEvent event) {
        return new EscalationEventResponse(
                event.getId(),
                event.getIncident() == null ? null : event.getIncident().getId(),
                event.getFromLevel(),
                event.getToLevel(),
                event.getFromUser() == null ? null : event.getFromUser().getId(),
                event.getFromUser() == null ? null : event.getFromUser().getName(),
                event.getToUser() == null ? null : event.getToUser().getId(),
                event.getToUser() == null ? null : event.getToUser().getName(),
                event.getReason(),
                event.getCreatedAt()
        );
    }
}
