package com.example.epager.project.dto;

import com.example.epager.project.SupportGroup;

import java.time.LocalDateTime;

public record SupportGroupResponse(
        Long id,
        Long projectId,
        String projectKey,
        String groupKey,
        String name,
        boolean active,
        LocalDateTime createdAt
) {
    public static SupportGroupResponse from(SupportGroup group) {
        return new SupportGroupResponse(
                group.getId(),
                group.getProject().getId(),
                group.getProject().getProjectKey(),
                group.getGroupKey(),
                group.getName(),
                group.isActive(),
                group.getCreatedAt()
        );
    }
}
