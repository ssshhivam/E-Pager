package com.example.epager.project.dto;

import com.example.epager.project.SupportGroupMember;

import java.time.LocalDateTime;

public record SupportGroupMemberResponse(
        Long id,
        Long groupId,
        String groupKey,
        Long userId,
        String userName,
        boolean active,
        LocalDateTime createdAt
) {
    public static SupportGroupMemberResponse from(SupportGroupMember member) {
        return new SupportGroupMemberResponse(
                member.getId(),
                member.getSupportGroup().getId(),
                member.getSupportGroup().getGroupKey(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.isActive(),
                member.getCreatedAt()
        );
    }
}
