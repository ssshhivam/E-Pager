package com.example.epager.alert;

import com.example.epager.incident.dto.IncidentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/{source}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidentResponse receiveAlert(
            @PathVariable String source,
            @RequestBody JsonNode payload
    ) {
        return IncidentResponse.from(alertService.processAlert(source, payload));
    }
}
