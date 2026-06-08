package com.example.epager.alert.dto;

public record UnifiedAlert(
        String source,
        String externalAlertId,
        String serviceName,
        String severity,
        String title,
        String description
) {
}
