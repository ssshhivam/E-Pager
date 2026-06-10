package com.example.epager.dashboard;

import com.example.epager.dashboard.dto.CountByStatusResponse;
import com.example.epager.dashboard.dto.DashboardResponse;
import com.example.epager.dashboard.dto.IncidentDashboardResponse;
import com.example.epager.dashboard.dto.NotificationDashboardResponse;
import com.example.epager.dashboard.dto.WebhookDashboardResponse;
import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentRepository;
import com.example.epager.incident.IncidentStatus;
import com.example.epager.incident.dto.IncidentResponse;
import com.example.epager.notification.NotificationLog;
import com.example.epager.notification.NotificationLogRepository;
import com.example.epager.notification.NotificationStatus;
import com.example.epager.notification.dto.NotificationLogResponse;
import com.example.epager.security.AuthenticatedUser;
import com.example.epager.user.AppRole;
import com.example.epager.webhook.WebhookAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DashboardService {

    private static final int RECENT_LIMIT = 10;

    private final IncidentRepository incidentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final WebhookAuditLogRepository webhookAuditLogRepository;

    public DashboardService(
            IncidentRepository incidentRepository,
            NotificationLogRepository notificationLogRepository,
            WebhookAuditLogRepository webhookAuditLogRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.webhookAuditLogRepository = webhookAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse buildDashboard(AuthenticatedUser user) {
        List<Incident> visibleIncidents = visibleIncidents(user);
        List<NotificationLog> visibleNotifications = visibleNotifications(user);
        LocalDateTime generatedAt = LocalDateTime.now();

        return new DashboardResponse(
                generatedAt,
                incidentSummary(visibleIncidents),
                notificationSummary(visibleNotifications),
                webhookSummary(generatedAt.minusHours(24)),
                visibleIncidents.stream()
                        .limit(RECENT_LIMIT)
                        .map(IncidentResponse::from)
                        .toList(),
                visibleNotifications.stream()
                        .limit(RECENT_LIMIT)
                        .map(NotificationLogResponse::from)
                        .toList()
        );
    }

    private List<Incident> visibleIncidents(AuthenticatedUser user) {
        if (user.role() == AppRole.ENGINEER) {
            return incidentRepository.findByAssignedUserIdOrderByCreatedAtDescIdDesc(user.id());
        }
        return incidentRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    private List<NotificationLog> visibleNotifications(AuthenticatedUser user) {
        if (user.role() == AppRole.ENGINEER) {
            return notificationLogRepository.findByRecipientIdOrderByCreatedAtDescIdDesc(user.id());
        }
        return notificationLogRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    private IncidentDashboardResponse incidentSummary(List<Incident> incidents) {
        return new IncidentDashboardResponse(
                incidents.size(),
                countIncidents(incidents, IncidentStatus.TRIGGERED),
                countIncidents(incidents, IncidentStatus.ACKNOWLEDGED),
                countIncidents(incidents, IncidentStatus.RESOLVED),
                incidents.stream().filter(incident -> incident.getNextEscalationAt() != null).count(),
                Arrays.stream(IncidentStatus.values())
                        .map(status -> new CountByStatusResponse(status.name(), countIncidents(incidents, status)))
                        .toList()
        );
    }

    private NotificationDashboardResponse notificationSummary(List<NotificationLog> notifications) {
        return new NotificationDashboardResponse(
                notifications.size(),
                countNotifications(notifications, NotificationStatus.QUEUED),
                countNotifications(notifications, NotificationStatus.SENT),
                countNotifications(notifications, NotificationStatus.RECEIVED),
                countNotifications(notifications, NotificationStatus.SEEN),
                countNotifications(notifications, NotificationStatus.FAILED),
                Arrays.stream(NotificationStatus.values())
                        .map(status -> new CountByStatusResponse(status.name(), countNotifications(notifications, status)))
                        .toList()
        );
    }

    private WebhookDashboardResponse webhookSummary(LocalDateTime from) {
        var logs = webhookAuditLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(from);
        return new WebhookDashboardResponse(
                logs.size(),
                logs.stream().filter(log -> log.isAccepted()).count(),
                logs.stream().filter(log -> !log.isAccepted()).count()
        );
    }

    private long countIncidents(List<Incident> incidents, IncidentStatus status) {
        return incidents.stream()
                .filter(incident -> status == incident.getStatus())
                .count();
    }

    private long countNotifications(List<NotificationLog> notifications, NotificationStatus status) {
        return notifications.stream()
                .filter(notification -> status == notification.getStatus())
                .count();
    }
}
