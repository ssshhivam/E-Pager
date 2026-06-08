package com.example.epager.notification.dto;

import com.example.epager.notification.NotificationChannel;
import com.example.epager.notification.NotificationLog;
import com.example.epager.notification.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationLogResponse(
        Long id,
        Long incidentId,
        Long recipientUserId,
        String recipientName,
        NotificationChannel channel,
        NotificationStatus status,
        String destination,
        String title,
        String message,
        String deepLink,
        String providerMessageId,
        String errorMessage,
        boolean delivered,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        LocalDateTime receivedAt,
        LocalDateTime seenAt,
        LocalDateTime failedAt
) {
    public static NotificationLogResponse from(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(),
                log.getIncident() == null ? null : log.getIncident().getId(),
                log.getRecipient() == null ? null : log.getRecipient().getId(),
                log.getRecipient() == null ? null : log.getRecipient().getName(),
                log.getChannel(),
                log.getStatus(),
                log.getDestination(),
                log.getTitle(),
                log.getMessage(),
                log.getDeepLink(),
                log.getProviderMessageId(),
                log.getErrorMessage(),
                log.isDelivered(),
                log.getCreatedAt(),
                log.getSentAt(),
                log.getReceivedAt(),
                log.getSeenAt(),
                log.getFailedAt()
        );
    }
}
