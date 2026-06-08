package com.example.epager.alert;

import com.example.epager.alert.dto.DynatraceAlertRequest;
import com.example.epager.alert.dto.GrafanaAlertRequest;
import com.example.epager.incident.dto.IncidentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/grafana")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidentResponse receiveGrafanaAlert(@Valid @RequestBody GrafanaAlertRequest request) {
        return IncidentResponse.from(alertService.processGrafanaAlert(request));
    }

    @PostMapping("/dynatrace")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidentResponse receiveDynatraceAlert(@Valid @RequestBody DynatraceAlertRequest request) {
        return IncidentResponse.from(alertService.processDynatraceAlert(request));
    }
}
