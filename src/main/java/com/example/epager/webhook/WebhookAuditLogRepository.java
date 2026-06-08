package com.example.epager.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WebhookAuditLogRepository extends JpaRepository<WebhookAuditLog, Long> {

    List<WebhookAuditLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
}
