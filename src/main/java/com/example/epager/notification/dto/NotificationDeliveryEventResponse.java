package com.example.epager.notification.dto;

import com.example.epager.notification.NotificationDeliveryEvent;
import com.example.epager.notification.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationDeliveryEventResponse(
        Long id,
        Long notificationLogId,
        NotificationStatus status,
        String detail,
        String clientInfo,
        LocalDateTime createdAt
) {
    public static NotificationDeliveryEventResponse from(NotificationDeliveryEvent event) {
        return new NotificationDeliveryEventResponse(
                event.getId(),
                event.getNotificationLog() == null ? null : event.getNotificationLog().getId(),
                event.getStatus(),
                event.getDetail(),
                event.getClientInfo(),
                event.getCreatedAt()
        );
    }
}
