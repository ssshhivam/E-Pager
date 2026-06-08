package com.example.epager.escalation.dto;

import com.example.epager.escalation.EscalationLevel;

public record EscalationLevelResponse(
        Long id,
        Integer levelNumber,
        Long userId,
        String userName,
        Integer waitMinutes
) {
    public static EscalationLevelResponse from(EscalationLevel level) {
        return new EscalationLevelResponse(
                level.getId(),
                level.getLevelNumber(),
                level.getUser().getId(),
                level.getUser().getName(),
                level.getWaitMinutes()
        );
    }
}
