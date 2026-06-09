package com.example.epager.security.dto;

import com.example.epager.user.AppRole;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Long userId,
        String name,
        String email,
        AppRole role
) {
}
