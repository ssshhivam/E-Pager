package com.example.epager.incident;

import com.example.epager.alert.dto.UnifiedAlert;
import com.example.epager.escalation.EscalationService;
import com.example.epager.security.AuthenticatedUser;
import com.example.epager.user.AppRole;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AppUserRepository appUserRepository;
    private final EscalationService escalationService;

    public IncidentService(
            IncidentRepository incidentRepository,
            AppUserRepository appUserRepository,
            EscalationService escalationService
    ) {
        this.incidentRepository = incidentRepository;
        this.appUserRepository = appUserRepository;
        this.escalationService = escalationService;
    }

    @Transactional
    public Incident createOrUpdateIncident(UnifiedAlert alert) {
        if (alert.externalAlertId() != null && !alert.externalAlertId().isBlank()) {
            return incidentRepository.findBySourceAndExternalAlertIdAndStatusNot(
                            alert.source(),
                            alert.externalAlertId(),
                            IncidentStatus.RESOLVED
                    )
                    .orElseGet(() -> createIncident(alert));
        }

        return createIncident(alert);
    }

    @Transactional(readOnly = true)
    public List<Incident> findAll() {
        return incidentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Incident> findVisibleTo(AuthenticatedUser user) {
        if (user.role() == AppRole.ENGINEER) {
            return incidentRepository.findByAssignedUserId(user.id());
        }
        return incidentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Incident findById(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: " + incidentId));
    }

    @Transactional(readOnly = true)
    public Incident findVisibleById(Long incidentId, AuthenticatedUser user) {
        Incident incident = findById(incidentId);
        assertCanAccessIncident(incident, user);
        return incident;
    }

    @Transactional
    public Incident acknowledge(Long incidentId, Long userId) {
        Incident incident = findById(incidentId);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedBy(user);
        incident.setAcknowledgedAt(LocalDateTime.now());
        incident.setNextEscalationAt(null);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident acknowledge(Long incidentId, AuthenticatedUser user) {
        Incident incident = findById(incidentId);
        assertCanAccessIncident(incident, user);

        AppUser acknowledgingUser = appUserRepository.findById(user.id())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + user.id()));
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedBy(acknowledgingUser);
        incident.setAcknowledgedAt(LocalDateTime.now());
        incident.setNextEscalationAt(null);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident resolve(Long incidentId) {
        Incident incident = findById(incidentId);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setNextEscalationAt(null);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident resolve(Long incidentId, AuthenticatedUser user) {
        Incident incident = findById(incidentId);
        assertCanAccessIncident(incident, user);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setNextEscalationAt(null);
        return incidentRepository.save(incident);
    }

    private Incident createIncident(UnifiedAlert alert) {
        Incident incident = new Incident();
        incident.setSource(alert.source());
        incident.setExternalAlertId(alert.externalAlertId());
        incident.setProjectKey(alert.projectKey());
        incident.setGroupKey(alert.groupKey());
        incident.setServiceName(alert.serviceName());
        incident.setSeverity(alert.severity());
        incident.setTitle(alert.title());
        incident.setDescription(alert.description());
        incident.setStatus(IncidentStatus.TRIGGERED);
        incident.setCreatedAt(LocalDateTime.now());

        Incident savedIncident = incidentRepository.save(incident);
        escalationService.startEscalation(savedIncident);
        return savedIncident;
    }

    private void assertCanAccessIncident(Incident incident, AuthenticatedUser user) {
        if (user.role() != AppRole.ENGINEER) {
            return;
        }
        if (incident.getAssignedUser() == null || !user.id().equals(incident.getAssignedUser().getId())) {
            throw new AccessDeniedException("Engineer can access only assigned incidents");
        }
    }
}
