package com.example.epager.webhook;

import com.example.epager.webhook.dto.WebhookAuditLogResponse;
import com.example.epager.webhook.dto.WebhookSourceRequest;
import com.example.epager.webhook.dto.WebhookSourceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookSecurityService webhookSecurityService;

    public WebhookController(WebhookSecurityService webhookSecurityService) {
        this.webhookSecurityService = webhookSecurityService;
    }

    @GetMapping("/sources")
    public List<WebhookSourceResponse> listSources() {
        return webhookSecurityService.findAllSources().stream()
                .map(WebhookSourceResponse::from)
                .toList();
    }

    @PostMapping("/sources")
    public WebhookSourceResponse createSource(@Valid @RequestBody WebhookSourceRequest request) {
        return WebhookSourceResponse.from(webhookSecurityService.createSource(
                request.sourceName(),
                request.secretToken(),
                request.description(),
                request.enabled()
        ));
    }

    @GetMapping("/audit")
    public List<WebhookAuditLogResponse> listAuditLogs() {
        return webhookSecurityService.findRecentAuditLogs().stream()
                .map(WebhookAuditLogResponse::from)
                .toList();
    }
}
