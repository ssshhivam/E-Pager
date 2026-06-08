package com.example.epager.webhook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WebhookSecurityService {

    public static final String TOKEN_HEADER = "X-EPAGER-WEBHOOK-TOKEN";

    private final WebhookSourceRepository webhookSourceRepository;
    private final WebhookAuditLogRepository webhookAuditLogRepository;

    public WebhookSecurityService(
            WebhookSourceRepository webhookSourceRepository,
            WebhookAuditLogRepository webhookAuditLogRepository
    ) {
        this.webhookSourceRepository = webhookSourceRepository;
        this.webhookAuditLogRepository = webhookAuditLogRepository;
    }

    @Transactional
    public void validate(String sourceName, String suppliedToken, String remoteAddress) {
        WebhookSource source = webhookSourceRepository.findBySourceNameIgnoreCaseAndEnabledTrue(sourceName)
                .orElse(null);

        if (source == null) {
            audit(sourceName, false, "Webhook source is not configured or enabled", remoteAddress, hasText(suppliedToken));
            throw new WebhookAuthenticationException("Webhook source is not configured or enabled");
        }

        if (!tokenMatches(source.getSecretToken(), suppliedToken)) {
            audit(sourceName, false, "Invalid webhook token", remoteAddress, hasText(suppliedToken));
            throw new WebhookAuthenticationException("Invalid webhook token");
        }

        audit(sourceName, true, null, remoteAddress, true);
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

    private void audit(String sourceName, boolean accepted, String rejectionReason, String remoteAddress, boolean tokenPresent) {
        WebhookAuditLog auditLog = new WebhookAuditLog();
        auditLog.setSourceName(sourceName);
        auditLog.setAccepted(accepted);
        auditLog.setRejectionReason(rejectionReason);
        auditLog.setRemoteAddress(remoteAddress);
        auditLog.setTokenPresent(tokenPresent);
        auditLog.setCreatedAt(LocalDateTime.now());
        webhookAuditLogRepository.save(auditLog);
    }

    private boolean tokenMatches(String expectedToken, String suppliedToken) {
        if (!hasText(expectedToken) || !hasText(suppliedToken)) {
            return false;
        }
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, supplied);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
