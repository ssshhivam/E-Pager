package com.example.epager.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookSourceRepository extends JpaRepository<WebhookSource, Long> {

    Optional<WebhookSource> findBySourceNameIgnoreCase(String sourceName);

    Optional<WebhookSource> findBySourceNameIgnoreCaseAndEnabledTrue(String sourceName);
}
