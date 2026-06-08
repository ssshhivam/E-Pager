package com.example.epager.alert;

import com.example.epager.alert.dto.UnifiedAlert;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class GrafanaAlertAdapter implements AlertSourceAdapter {

    @Override
    public String source() {
        return "grafana";
    }

    @Override
    public UnifiedAlert toUnifiedAlert(JsonNode payload) {
        JsonNode firstAlert = JsonAlertReader.first(payload, "alerts");
        JsonNode labels = firstAlert == null || firstAlert.path("labels").isMissingNode()
                ? payload.path("commonLabels")
                : firstAlert.path("labels");
        JsonNode annotations = firstAlert == null || firstAlert.path("annotations").isMissingNode()
                ? payload.path("commonAnnotations")
                : firstAlert.path("annotations");

        String title = JsonAlertReader.firstNonBlank(
                JsonAlertReader.text(annotations, "summary"),
                JsonAlertReader.text(payload, "title"),
                JsonAlertReader.text(labels, "alertname"),
                "Grafana alert"
        );

        return new UnifiedAlert(
                "GRAFANA",
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(firstAlert, "fingerprint"),
                        JsonAlertReader.text(labels, "alert_id"),
                        JsonAlertReader.generatedId()
                ),
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(labels, "service"),
                        JsonAlertReader.text(labels, "app"),
                        JsonAlertReader.text(labels, "job"),
                        "default"
                ),
                JsonAlertReader.firstNonBlank(JsonAlertReader.text(labels, "severity"), "warning"),
                title,
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(annotations, "description"),
                        JsonAlertReader.text(payload, "message"),
                        JsonAlertReader.text(payload, "externalURL"),
                        title
                )
        );
    }
}
