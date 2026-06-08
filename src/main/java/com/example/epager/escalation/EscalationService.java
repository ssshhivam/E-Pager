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
    private final EscalationEventRepository escalationEventRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    public EscalationService(
            EscalationPolicyRepository escalationPolicyRepository,
            EscalationEventRepository escalationEventRepository,
            IncidentRepository incidentRepository,
            NotificationService notificationService
    ) {
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.escalationEventRepository = escalationEventRepository;
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void startEscalation(Incident incident) {
        Optional<EscalationPolicy> policy = findPolicy(incident);

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
        Optional<EscalationPolicy> policy = findPolicy(incident);

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
        Integer fromLevel = incident.getCurrentEscalationLevel();
        var fromUser = incident.getAssignedUser();
        incident.setAssignedUser(level.getUser());
        incident.setCurrentEscalationLevel(level.getLevelNumber());
        incident.setNextEscalationAt(LocalDateTime.now().plusMinutes(level.getWaitMinutes()));
        Incident savedIncident = incidentRepository.save(incident);
        recordEvent(savedIncident, fromLevel, level, fromUser);
        notificationService.notifyUser(savedIncident, level.getUser());
    }

    private void recordEvent(Incident incident, Integer fromLevel, EscalationLevel level, com.example.epager.user.AppUser fromUser) {
        EscalationEvent event = new EscalationEvent();
        event.setIncident(incident);
        event.setFromLevel(fromLevel);
        event.setToLevel(level.getLevelNumber());
        event.setFromUser(fromUser);
        event.setToUser(level.getUser());
        event.setReason(fromLevel == null || fromLevel == 0 ? "INITIAL_ASSIGNMENT" : "NO_ACK_ESCALATION");
        event.setCreatedAt(LocalDateTime.now());
        escalationEventRepository.save(event);
    }

    private Optional<EscalationPolicy> findPolicy(Incident incident) {
        Optional<EscalationPolicy> projectGroupPolicy = escalationPolicyRepository
                .findByProjectKeyIgnoreCaseAndGroupKeyIgnoreCaseAndEnabledTrue(
                        incident.getProjectKey(),
                        incident.getGroupKey()
                );

        return projectGroupPolicy.isPresent()
                ? projectGroupPolicy
                : escalationPolicyRepository.findByServiceNameIgnoreCaseAndEnabledTrue(incident.getServiceName());
    }
}
