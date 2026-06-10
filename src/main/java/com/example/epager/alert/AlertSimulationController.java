package com.example.epager.alert;

import com.example.epager.alert.dto.SimulatedAlertResponse;
import com.example.epager.incident.Incident;
import com.example.epager.incident.dto.IncidentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/testing/alerts")
public class AlertSimulationController {

    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    public AlertSimulationController(AlertService alertService, ObjectMapper objectMapper) {
        this.alertService = alertService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{source}/critical")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public SimulatedAlertResponse simulateCriticalAlert(@PathVariable String source) {
        JsonNode payload = switch (source.toLowerCase()) {
            case "dynatrace" -> dynatraceCriticalPayload();
            case "grafana" -> grafanaCriticalPayload();
            default -> throw new IllegalArgumentException("Unsupported simulation source: " + source);
        };
        Incident incident = alertService.processAlert(source, payload);
        return new SimulatedAlertResponse(
                source.toLowerCase(),
                "critical",
                simulatedExternalPostTarget(source),
                payload,
                IncidentResponse.from(incident)
        );
    }

    private JsonNode dynatraceCriticalPayload() {
        String id = "DT-SIM-" + Instant.now().toEpochMilli();
        ObjectNode tags = objectMapper.createObjectNode()
                .put("projectKey", "payments")
                .put("groupKey", "primary-support")
                .put("service", "payments")
                .put("severity", "critical");

        return objectMapper.createObjectNode()
                .put("problemId", id)
                .put("problemTitle", "Payments service failure rate is critical")
                .put("problemImpact", "SERVICE")
                .put("severityLevel", "critical")
                .put("state", "OPEN")
                .put("impactedEntity", "payments")
                .put("problemDetailsText", "Dynatrace simulation: payments service error rate crossed the critical threshold.")
                .set("tags", tags);
    }

    private JsonNode grafanaCriticalPayload() {
        String id = "GF-SIM-" + Instant.now().toEpochMilli();
        ObjectNode labels = objectMapper.createObjectNode()
                .put("projectKey", "payments")
                .put("groupKey", "primary-support")
                .put("service", "payments")
                .put("severity", "critical")
                .put("alertname", "PaymentsHighErrorRate");

        ObjectNode annotations = objectMapper.createObjectNode()
                .put("summary", "Payments error rate is critical")
                .put("description", "Grafana simulation: payments error rate crossed the critical threshold.");

        ObjectNode alert = objectMapper.createObjectNode()
                .put("status", "firing")
                .put("fingerprint", id);
        alert.set("labels", labels);
        alert.set("annotations", annotations);

        ObjectNode payload = objectMapper.createObjectNode()
                .put("receiver", "epager")
                .put("status", "firing")
                .put("title", "Payments error rate is critical")
                .put("message", "Grafana simulation alert for E-Pager");
        payload.set("commonLabels", labels);
        payload.set("alerts", objectMapper.createArrayNode().add(alert));
        return payload;
    }

    private String simulatedExternalPostTarget(String source) {
        return switch (source.toLowerCase()) {
            case "dynatrace" -> "/gateway/webhooks/dynatrace";
            case "grafana" -> "/api/alerts/grafana";
            default -> "/api/alerts/" + source.toLowerCase();
        };
    }
}
