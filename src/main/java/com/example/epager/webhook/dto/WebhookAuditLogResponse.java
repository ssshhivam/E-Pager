package com.example.epager.webhook.dto;

import com.example.epager.webhook.WebhookAuditLog;

import java.time.LocalDateTime;

public record WebhookAuditLogResponse(
        Long id,
        String sourceName,
        boolean accepted,
        String rejectionReason,
        String remoteAddress,
        boolean tokenPresent,
        boolean signaturePresent,
        boolean timestampPresent,
        LocalDateTime createdAt
) {
    public static WebhookAuditLogResponse from(WebhookAuditLog auditLog) {
        return new WebhookAuditLogResponse(
                auditLog.getId(),
                auditLog.getSourceName(),
                auditLog.isAccepted(),
                auditLog.getRejectionReason(),
                auditLog.getRemoteAddress(),
                auditLog.isTokenPresent(),
                auditLog.isSignaturePresent(),
                auditLog.isTimestampPresent(),
                auditLog.getCreatedAt()
        );
    }
}
