package com.example.epager.notification;

public record NotificationResult(
        boolean delivered,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationResult delivered(String providerMessageId) {
        return new NotificationResult(true, providerMessageId, null);
    }

    public static NotificationResult failed(String errorMessage) {
        return new NotificationResult(false, null, errorMessage);
    }
}
