package com.example.epager.escalation;

import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentRepository;
import com.example.epager.incident.IncidentStatus;
import com.example.epager.notification.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EscalationService {

    private final EscalationPolicyRepository escalationPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    public EscalationService(
            EscalationPolicyRepository escalationPolicyRepository,
            IncidentRepository incidentRepository,
            NotificationService notificationService
    ) {
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void startEscalation(Incident incident) {
        Optional<EscalationPolicy> policy = escalationPolicyRepository
                .findByServiceNameIgnoreCaseAndEnabledTrue(incident.getServiceName());

        if (policy.isEmpty() || policy.get().getLevels().isEmpty()) {
            incident.setNextEscalationAt(null);
            incidentRepository.save(incident);
            return;
        }

        notifyLevel(incident, policy.get().getLevels().get(0));
    }

    @Scheduled(fixedDelayString = "${epager.scheduler.escalation-check-rate-ms:60000}")
    @Transactional
    public void processPendingEscalations() {
        List<Incident> incidents = incidentRepository.findByStatusAndNextEscalationAtLessThanEqual(
                IncidentStatus.TRIGGERED,
                LocalDateTime.now()
        );

        incidents.forEach(this::escalateIfNeeded);
    }

    private void escalateIfNeeded(Incident incident) {
        Optional<EscalationPolicy> policy = escalationPolicyRepository
                .findByServiceNameIgnoreCaseAndEnabledTrue(incident.getServiceName());

        if (policy.isEmpty()) {
            incident.setNextEscalationAt(null);
            incidentRepository.save(incident);
            return;
        }

        int nextLevelNumber = incident.getCurrentEscalationLevel() + 1;
        Optional<EscalationLevel> nextLevel = policy.get().getLevels().stream()
                .filter(level -> level.getLevelNumber() == nextLevelNumber)
                .min(Comparator.comparing(EscalationLevel::getLevelNumber));

        nextLevel.ifPresentOrElse(
                level -> notifyLevel(incident, level),
                () -> {
                    incident.setNextEscalationAt(null);
                    incidentRepository.save(incident);
                }
        );
    }

    private void notifyLevel(Incident incident, EscalationLevel level) {
        incident.setAssignedUser(level.getUser());
        incident.setCurrentEscalationLevel(level.getLevelNumber());
        incident.setNextEscalationAt(LocalDateTime.now().plusMinutes(level.getWaitMinutes()));
        Incident savedIncident = incidentRepository.save(incident);
        notificationService.notifyUser(savedIncident, level.getUser());
    }
}
