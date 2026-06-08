package com.example.epager.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentService;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final IncidentService incidentService;
    private final AlertSourceAdapterRegistry adapterRegistry;

    public AlertService(IncidentService incidentService, AlertSourceAdapterRegistry adapterRegistry) {
        this.incidentService = incidentService;
        this.adapterRegistry = adapterRegistry;
    }

    public Incident processAlert(String source, JsonNode payload) {
        return incidentService.createOrUpdateIncident(
                adapterRegistry.getAdapter(source).toUnifiedAlert(payload)
        );
    }
}
