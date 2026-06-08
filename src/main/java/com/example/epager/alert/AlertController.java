package com.example.epager.alert;

import com.example.epager.incident.dto.IncidentResponse;
import com.example.epager.webhook.WebhookSecurityService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final WebhookSecurityService webhookSecurityService;

    public AlertController(AlertService alertService, WebhookSecurityService webhookSecurityService) {
        this.alertService = alertService;
        this.webhookSecurityService = webhookSecurityService;
    }

    @PostMapping("/{source}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidentResponse receiveAlert(
            @PathVariable String source,
            @RequestHeader(value = WebhookSecurityService.TOKEN_HEADER, required = false) String token,
            @RequestBody JsonNode payload,
            HttpServletRequest request
    ) {
        webhookSecurityService.validate(source, token, request.getRemoteAddr());
        return IncidentResponse.from(alertService.processAlert(source, payload));
    }
}
