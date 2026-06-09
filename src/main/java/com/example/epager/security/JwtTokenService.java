package com.example.epager.security;

import com.example.epager.user.AppRole;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long ttlSeconds;

    public JwtTokenService(
            @Value("${epager.security.jwt-secret:change-this-local-dev-secret}") String secret,
            @Value("${epager.security.jwt-ttl-seconds:28800}") long ttlSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.ttlSeconds = ttlSeconds;
    }

    public TokenPair createAccessToken(Long userId, String email, AppRole role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new TokenPair(token, expiresAt);
    }

    public Optional<TokenClaims> validate(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            AppRole role = AppRole.valueOf(claims.get("role", String.class));
            return Optional.of(new TokenClaims(userId, email, role));
        } catch (IllegalArgumentException | JwtException exception) {
            return Optional.empty();
        }
    }

    private byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record TokenPair(String token, Instant expiresAt) {
    }

    public record TokenClaims(Long userId, String email, AppRole role) {
    }
}
