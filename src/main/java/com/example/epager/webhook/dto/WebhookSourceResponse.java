package com.example.epager.webhook.dto;

import com.example.epager.webhook.WebhookSource;

import java.time.LocalDateTime;

public record WebhookSourceResponse(
        Long id,
        String sourceName,
        String description,
        boolean enabled,
        LocalDateTime createdAt
) {
    public static WebhookSourceResponse from(WebhookSource source) {
        return new WebhookSourceResponse(
                source.getId(),
                source.getSourceName(),
                source.getDescription(),
                source.isEnabled(),
                source.getCreatedAt()
        );
    }
}
