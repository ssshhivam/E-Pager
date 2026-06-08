package com.example.epager.notification;

public record NotificationRequest(
        Long incidentId,
        Long userId,
        NotificationChannel channel,
        String destination,
        String title,
        String message,
        String severity,
        String deepLink
) {
}
