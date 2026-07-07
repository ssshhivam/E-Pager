package com.example.epager.escalation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.epager.incident.Incident;
import com.example.epager.incident.IncidentRepository;
import com.example.epager.incident.IncidentStatus;
import com.example.epager.notification.NotificationService;
import com.example.epager.user.AppUser;
import com.example.epager.user.roster.AppUserRosterRepository;
import com.example.epager.user.roster.Shift;
import com.example.epager.user.roster.ShiftRepository;

@Service
public class EscalationService {

    private final EscalationPolicyRepository escalationPolicyRepository;
    private final EscalationEventRepository escalationEventRepository;
    private final EscalationLevelUserRepository escalationLevelUserRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final ShiftRepository shiftRepository;
    private final AppUserRosterRepository rosterRepository;

    public EscalationService(
            EscalationPolicyRepository escalationPolicyRepository,
            EscalationEventRepository escalationEventRepository,
            EscalationLevelUserRepository escalationLevelUserRepository,
            IncidentRepository incidentRepository,
            NotificationService notificationService,
            ShiftRepository shiftRepository,
            AppUserRosterRepository rosterRepository
    ) {
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.escalationEventRepository = escalationEventRepository;
        this.escalationLevelUserRepository = escalationLevelUserRepository;
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
        this.shiftRepository = shiftRepository;
        this.rosterRepository = rosterRepository;
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
		List<AppUser> currentShiftUsers = getCurrentShiftUsers(
				escalationLevelUserRepository.findUsersByEscalationLevelId(level.getId()));
		incident.setCurrentEscalationLevel(level.getLevelNumber());
		incident.setNextEscalationAt(LocalDateTime.now().plusMinutes(level.getWaitMinutes()));
		Incident savedIncident = incidentRepository.save(incident);
		recordEvent(savedIncident, fromLevel, level, fromUser);// need changes here
		notificationService.notifyUser(savedIncident, currentShiftUsers);
	}

    private List<AppUser> getCurrentShiftUsers(List<AppUser> escalationUsers) {

        Shift currentShift = getCurrentShift();

        List<AppUser> shiftUsers = rosterRepository.findUsersByShift(
                currentShift.getId(),
                LocalDate.now());

        Set<Long> shiftUserIds = shiftUsers.stream()
                .map(AppUser::getId)
                .collect(Collectors.toSet());

        return escalationUsers.stream()
                .filter(user -> shiftUserIds.contains(user.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    private Shift getCurrentShift() {

        return shiftRepository
                .findCurrentShift(LocalTime.now())
                .orElseThrow(() ->
                        new RuntimeException("No shift configured for current time."));
    }
    
    private void recordEvent(Incident incident, Integer fromLevel, EscalationLevel level, AppUser appUser) {
        EscalationEvent event = new EscalationEvent();
        event.setIncident(incident);
        event.setFromLevel(fromLevel);
        event.setToLevel(level.getLevelNumber());
        event.setToUser(appUser);
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
