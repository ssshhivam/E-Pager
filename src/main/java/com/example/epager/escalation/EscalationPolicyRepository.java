package com.example.epager.escalation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscalationPolicyRepository extends JpaRepository<EscalationPolicy, Long> {

    Optional<EscalationPolicy> findByProjectKeyIgnoreCaseAndGroupKeyIgnoreCaseAndEnabledTrue(
            String projectKey,
            String groupKey
    );

    Optional<EscalationPolicy> findByServiceNameIgnoreCaseAndEnabledTrue(String serviceName);
}
