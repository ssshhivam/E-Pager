package com.example.epager.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findBySourceAndExternalAlertIdAndStatusNot(
            String source,
            String externalAlertId,
            IncidentStatus status
    );

    List<Incident> findByStatusAndNextEscalationAtLessThanEqual(
            IncidentStatus status,
            LocalDateTime now
    );

    List<Incident> findByAssignedUserId(Long userId);
}
