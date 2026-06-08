package com.example.epager.alert;

import com.example.epager.alert.dto.DynatraceAlertRequest;
import com.example.epager.alert.dto.GrafanaAlertRequest;
import com.example.epager.alert.dto.UnifiedAlert;
import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AlertService {

    private final IncidentService incidentService;

    public AlertService(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    public Incident processGrafanaAlert(GrafanaAlertRequest request) {
        Map<String, String> labels = request.getCommonLabels();
        Map<String, String> annotations = request.getCommonAnnotations();
        GrafanaAlertRequest.GrafanaAlertItem firstAlert = request.getAlerts() == null || request.getAlerts().isEmpty()
                ? null
                : request.getAlerts().get(0);

        if (firstAlert != null) {
            labels = firstAlert.getLabels() == null ? labels : firstAlert.getLabels();
            annotations = firstAlert.getAnnotations() == null ? annotations : firstAlert.getAnnotations();
        }

        String title = firstNonBlank(
                read(annotations, "summary"),
                request.getTitle(),
                read(labels, "alertname"),
                "Grafana alert"
        );

        UnifiedAlert alert = new UnifiedAlert(
                "GRAFANA",
                firstNonBlank(firstAlert == null ? null : firstAlert.getFingerprint(), read(labels, "alert_id"), UUID.randomUUID().toString()),
                firstNonBlank(read(labels, "service"), read(labels, "app"), read(labels, "job"), "default"),
                firstNonBlank(read(labels, "severity"), "warning"),
                title,
                firstNonBlank(read(annotations, "description"), request.getMessage(), request.getExternalURL(), title)
        );

        return incidentService.createOrUpdateIncident(alert);
    }

    public Incident processDynatraceAlert(DynatraceAlertRequest request) {
        UnifiedAlert alert = new UnifiedAlert(
                "DYNATRACE",
                firstNonBlank(request.getProblemId(), UUID.randomUUID().toString()),
                firstNonBlank(read(request.getTags(), "service"), read(request.getTags(), "app"), request.getImpactedEntity(), "default"),
                firstNonBlank(request.getSeverityLevel(), "warning"),
                firstNonBlank(request.getProblemTitle(), "Dynatrace problem"),
                firstNonBlank(request.getProblemDetailsText(), request.getProblemTitle(), "Dynatrace problem")
        );

        return incidentService.createOrUpdateIncident(alert);
    }

    private String read(Map<String, String> values, String key) {
        return values == null ? null : values.get(key);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
