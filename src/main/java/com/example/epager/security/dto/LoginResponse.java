package com.example.epager.security.dto;

import com.example.epager.user.AppRole;

import java.time.Instant;
import java.time.LocalDateTime;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt,
        Long userId,
        String name,
        String email,
        AppRole role
) {
}
