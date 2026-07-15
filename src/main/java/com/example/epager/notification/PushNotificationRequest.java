package com.example.epager.notification;

import java.util.List;

public record PushNotificationRequest(List<String> tokens, String title, String message, String severity,
		String deepLink, Long incidentId) {
}