package com.example.epager.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank String projectKey,
        @NotBlank String name,
        String description,
        Boolean active
) {
}
