package com.example.epager.project.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportGroupRequest(
        @NotBlank String groupKey,
        @NotBlank String name,
        Boolean active
) {
}
