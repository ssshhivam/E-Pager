package com.example.epager.incident;

import com.example.epager.incident.dto.AcknowledgeIncidentRequest;
import com.example.epager.incident.dto.IncidentResponse;
import com.example.epager.security.CurrentUser;
import com.example.epager.user.AppRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final CurrentUser currentUser;

    public IncidentController(IncidentService incidentService, CurrentUser currentUser) {
        this.incidentService = incidentService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<IncidentResponse> listIncidents() {
        return incidentService.findVisibleTo(currentUser.require()).stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @GetMapping("/{incidentId}")
    public IncidentResponse getIncident(@PathVariable Long incidentId) {
        return IncidentResponse.from(incidentService.findVisibleById(incidentId, currentUser.require()));
    }

    @PostMapping("/{incidentId}/acknowledge")
    public IncidentResponse acknowledge(
            @PathVariable Long incidentId,
            @RequestBody(required = false) AcknowledgeIncidentRequest request
    ) {
        var user = currentUser.require();
        if (user.role() == AppRole.ENGINEER) {
            return IncidentResponse.from(incidentService.acknowledge(incidentId, user));
        }
        if (request == null || request.userId() == null) {
            throw new IllegalArgumentException("userId is required when acknowledging as admin or manager");
        }
        return IncidentResponse.from(incidentService.acknowledge(incidentId, request.userId()));
    }

    @PostMapping("/{incidentId}/resolve")
    public IncidentResponse resolve(@PathVariable Long incidentId) {
        return IncidentResponse.from(incidentService.resolve(incidentId, currentUser.require()));
    }
}
