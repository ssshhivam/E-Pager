package com.example.epager.alert;

import com.example.epager.incident.dto.IncidentResponse;
import com.example.epager.webhook.WebhookSecurityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public AlertController(
            AlertService alertService,
            WebhookSecurityService webhookSecurityService,
            ObjectMapper objectMapper
    ) {
        this.alertService = alertService;
        this.webhookSecurityService = webhookSecurityService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{source}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidentResponse receiveAlert(
            @PathVariable String source,
            @RequestHeader(value = WebhookSecurityService.SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = WebhookSecurityService.TIMESTAMP_HEADER, required = false) String timestamp,
            @RequestBody String rawPayload,
            HttpServletRequest request
    ) throws JsonProcessingException {
        webhookSecurityService.validate(source, signature, timestamp, rawPayload, request.getRemoteAddr());
        JsonNode payload = objectMapper.readTree(rawPayload);
        return IncidentResponse.from(alertService.processAlert(source, payload));
    }
}
