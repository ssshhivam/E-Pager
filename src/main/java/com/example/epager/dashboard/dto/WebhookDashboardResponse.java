package com.example.epager.dashboard.dto;

public record WebhookDashboardResponse(
        long last24HoursTotal,
        long last24HoursAccepted,
        long last24HoursRejected
) {
}
