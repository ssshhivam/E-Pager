package com.example.epager.notification;

import java.util.List;

public record NotificationRequest(
        Long notificationLogId,
        Long incidentId,
        Long userId,
        NotificationChannel channel,
        List<String> destination,
        String title,
        String message,
        String severity,
        String deepLink
) {
}
