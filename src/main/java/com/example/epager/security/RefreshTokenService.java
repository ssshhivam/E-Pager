package com.example.epager.security;

import com.example.epager.user.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${epager.security.refresh-token-ttl-seconds:604800}") long ttlSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public IssuedRefreshToken issue(AppUser user) {
        String token = randomToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(token));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(token, saved.getExpiresAt());
    }

    @Transactional
    public AppUser consumeAndRotate(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(refreshTokenValue))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
        if (!refreshToken.isActive()) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getUser();
    }

    @Transactional
    public void revokeActiveTokens(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findByUserAndRevokedAtIsNull(user).forEach(token -> {
            token.setRevokedAt(now);
            refreshTokenRepository.save(token);
        });
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(String token, LocalDateTime expiresAt) {
    }
}
