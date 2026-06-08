package com.example.epager.webhook.dto;

import jakarta.validation.constraints.NotBlank;

public record WebhookSourceRequest(
        @NotBlank String sourceName,
        @NotBlank String secretToken,
        String description,
        Boolean enabled
) {
}
