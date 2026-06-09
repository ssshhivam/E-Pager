package com.example.epager.security;

import com.example.epager.user.AppRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final long ttlSeconds;

    public JwtTokenService(
            @Value("${epager.security.jwt-secret:change-this-local-dev-secret}") String secret,
            @Value("${epager.security.jwt-ttl-seconds:28800}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(Long userId, String email, AppRole role) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"" + userId + "\",\"email\":\"" + escape(email)
                + "\",\"role\":\"" + role.name() + "\",\"exp\":" + expiresAt + "}");
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    public Optional<TokenClaims> validate(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String signingInput = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(signingInput).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        Long userId = longClaim(payload, "sub").orElse(null);
        String email = stringClaim(payload, "email").orElse(null);
        AppRole role = stringClaim(payload, "role").map(AppRole::valueOf).orElse(null);
        long exp = longClaim(payload, "exp").orElse(0L);

        if (userId == null || email == null || role == null || Instant.now().getEpochSecond() > exp) {
            return Optional.empty();
        }

        return Optional.of(new TokenClaims(userId, email, role));
    }

    private String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private Optional<String> stringClaim(String json, String claimName) {
        String marker = "\"" + claimName + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        start += marker.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? Optional.empty() : Optional.of(json.substring(start, end));
    }

    private Optional<Long> longClaim(String json, String claimName) {
        String quotedMarker = "\"" + claimName + "\":\"";
        int quotedStart = json.indexOf(quotedMarker);
        if (quotedStart >= 0) {
            int start = quotedStart + quotedMarker.length();
            int end = json.indexOf("\"", start);
            return parseLong(end < 0 ? "" : json.substring(start, end));
        }

        String marker = "\"" + claimName + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return parseLong(json.substring(start, end));
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record TokenClaims(Long userId, String email, AppRole role) {
    }
}
