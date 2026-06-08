package com.example.epager.project.dto;

import jakarta.validation.constraints.NotNull;

public record AddGroupMemberRequest(@NotNull Long userId) {
}
