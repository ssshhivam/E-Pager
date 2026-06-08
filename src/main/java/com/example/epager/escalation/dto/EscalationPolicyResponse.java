package com.example.epager.escalation.dto;

import com.example.epager.escalation.EscalationPolicy;

import java.util.List;

public record EscalationPolicyResponse(
        Long id,
        String serviceName,
        boolean enabled,
        List<EscalationLevelResponse> levels
) {
    public static EscalationPolicyResponse from(EscalationPolicy policy) {
        return new EscalationPolicyResponse(
                policy.getId(),
                policy.getServiceName(),
                policy.isEnabled(),
                policy.getLevels().stream()
                        .map(EscalationLevelResponse::from)
                        .toList()
        );
    }
}
