package com.example.epager.escalation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EscalationEventRepository extends JpaRepository<EscalationEvent, Long> {

    List<EscalationEvent> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(LocalDateTime from);
}
