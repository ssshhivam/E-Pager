package com.example.epager.dashboard.dto;

import com.example.epager.incident.dto.IncidentResponse;
import com.example.epager.notification.dto.NotificationLogResponse;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        LocalDateTime generatedAt,
        IncidentDashboardResponse incidents,
        NotificationDashboardResponse notifications,
        WebhookDashboardResponse webhooks,
        List<IncidentResponse> recentIncidents,
        List<NotificationLogResponse> recentNotifications
) {
}
