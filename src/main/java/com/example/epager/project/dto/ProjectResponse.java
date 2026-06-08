package com.example.epager.project.dto;

import com.example.epager.project.Project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String projectKey,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getProjectKey(),
                project.getName(),
                project.getDescription(),
                project.isActive(),
                project.getCreatedAt()
        );
    }
}
