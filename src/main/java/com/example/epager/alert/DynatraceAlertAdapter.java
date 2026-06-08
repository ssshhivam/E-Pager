package com.example.epager.alert;

import com.example.epager.alert.dto.UnifiedAlert;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class DynatraceAlertAdapter implements AlertSourceAdapter {

    @Override
    public String source() {
        return "dynatrace";
    }

    @Override
    public UnifiedAlert toUnifiedAlert(JsonNode payload) {
        return new UnifiedAlert(
                "DYNATRACE",
                JsonAlertReader.firstNonBlank(JsonAlertReader.text(payload, "problemId"), JsonAlertReader.generatedId()),
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(payload, "tags", "projectKey"),
                        JsonAlertReader.text(payload, "tags", "project"),
                        "payments"
                ),
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(payload, "tags", "groupKey"),
                        JsonAlertReader.text(payload, "tags", "support_group"),
                        JsonAlertReader.text(payload, "tags", "group"),
                        "primary-support"
                ),
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(payload, "tags", "service"),
                        JsonAlertReader.text(payload, "tags", "app"),
                        JsonAlertReader.text(payload, "impactedEntity"),
                        "default"
                ),
                JsonAlertReader.firstNonBlank(JsonAlertReader.text(payload, "severityLevel"), "warning"),
                JsonAlertReader.firstNonBlank(JsonAlertReader.text(payload, "problemTitle"), "Dynatrace problem"),
                JsonAlertReader.firstNonBlank(
                        JsonAlertReader.text(payload, "problemDetailsText"),
                        JsonAlertReader.text(payload, "problemTitle"),
                        "Dynatrace problem"
                )
        );
    }
}
