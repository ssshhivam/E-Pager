package com.example.epager.webhook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;

@Service
public class WebhookSecurityService {

    public static final String SIGNATURE_HEADER = "X-EPAGER-SIGNATURE";
    public static final String TIMESTAMP_HEADER = "X-EPAGER-TIMESTAMP";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final WebhookSourceRepository webhookSourceRepository;
    private final WebhookAuditLogRepository webhookAuditLogRepository;

    public WebhookSecurityService(
            WebhookSourceRepository webhookSourceRepository,
            WebhookAuditLogRepository webhookAuditLogRepository
    ) {
        this.webhookSourceRepository = webhookSourceRepository;
        this.webhookAuditLogRepository = webhookAuditLogRepository;
    }

    @Transactional(noRollbackFor = WebhookAuthenticationException.class)
    public void validate(
            String sourceName,
            String suppliedSignature,
            String suppliedTimestamp,
            String rawPayload,
            String remoteAddress
    ) {
        WebhookSource source = webhookSourceRepository.findBySourceNameIgnoreCaseAndEnabledTrue(sourceName)
                .orElse(null);

        if (source == null) {
            audit(sourceName, false, "Webhook source is not configured or enabled", remoteAddress, suppliedSignature, suppliedTimestamp);
            throw new WebhookAuthenticationException("Webhook source is not configured or enabled");
        }

        if (!hasText(suppliedTimestamp)) {
            audit(sourceName, false, "Missing HMAC timestamp", remoteAddress, suppliedSignature, suppliedTimestamp);
            throw new WebhookAuthenticationException("Missing HMAC timestamp");
        }

        if (!timestampIsRecent(suppliedTimestamp)) {
            audit(sourceName, false, "Webhook timestamp is outside allowed window", remoteAddress, suppliedSignature, suppliedTimestamp);
            throw new WebhookAuthenticationException("Webhook timestamp is outside allowed window");
        }

        if (!hasText(suppliedSignature)) {
            audit(sourceName, false, "Missing HMAC signature", remoteAddress, suppliedSignature, suppliedTimestamp);
            throw new WebhookAuthenticationException("Missing HMAC signature");
        }

        if (!signatureMatches(source.getSecretToken(), suppliedTimestamp, rawPayload, suppliedSignature)) {
            audit(sourceName, false, "Invalid HMAC signature", remoteAddress, suppliedSignature, suppliedTimestamp);
            throw new WebhookAuthenticationException("Invalid HMAC signature");
        }

        audit(sourceName, true, null, remoteAddress, suppliedSignature, suppliedTimestamp);
    }

    public List<WebhookSource> findAllSources() {
        return webhookSourceRepository.findAll();
    }

    public List<WebhookAuditLog> findRecentAuditLogs() {
        return webhookAuditLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime.now().minusDays(7));
    }

    @Transactional
    public WebhookSource createSource(String sourceName, String secretToken, String description, Boolean enabled) {
        WebhookSource source = webhookSourceRepository.findBySourceNameIgnoreCase(sourceName)
                .orElseGet(WebhookSource::new);
        source.setSourceName(sourceName);
        source.setSecretToken(secretToken);
        source.setDescription(description);
        source.setEnabled(enabled == null || enabled);
        if (source.getCreatedAt() == null) {
            source.setCreatedAt(LocalDateTime.now());
        }
        return webhookSourceRepository.save(source);
    }

    private void audit(
            String sourceName,
            boolean accepted,
            String rejectionReason,
            String remoteAddress,
            String suppliedSignature,
            String suppliedTimestamp
    ) {
        WebhookAuditLog auditLog = new WebhookAuditLog();
        auditLog.setSourceName(sourceName);
        auditLog.setAccepted(accepted);
        auditLog.setRejectionReason(rejectionReason);
        auditLog.setRemoteAddress(remoteAddress);
        auditLog.setTokenPresent(false);
        auditLog.setSignaturePresent(hasText(suppliedSignature));
        auditLog.setTimestampPresent(hasText(suppliedTimestamp));
        auditLog.setCreatedAt(LocalDateTime.now());
        webhookAuditLogRepository.save(auditLog);
    }

    private boolean timestampIsRecent(String suppliedTimestamp) {
        try {
            Instant timestamp = Instant.parse(suppliedTimestamp);
            Duration age = Duration.between(timestamp, Instant.now()).abs();
            return age.compareTo(MAX_CLOCK_SKEW) <= 0;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private boolean signatureMatches(
            String secret,
            String suppliedTimestamp,
            String rawPayload,
            String suppliedSignature
    ) {
        if (!hasText(secret) || rawPayload == null) {
            return false;
        }

        String expectedSignature = "sha256=" + hmacSha256Hex(secret, suppliedTimestamp + ":" + rawPayload);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                normalizeSignature(suppliedSignature).getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to calculate webhook HMAC signature", exception);
        }
    }

    private String normalizeSignature(String suppliedSignature) {
        String signature = suppliedSignature.trim();
        return signature.startsWith("sha256=") ? signature : "sha256=" + signature;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
