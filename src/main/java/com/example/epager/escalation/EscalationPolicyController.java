package com.example.epager.escalation;

import com.example.epager.escalation.dto.EscalationLevelRequest;
import com.example.epager.escalation.dto.EscalationPolicyRequest;
import com.example.epager.escalation.dto.EscalationPolicyResponse;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/escalation-policies")
public class EscalationPolicyController {

    private final EscalationPolicyRepository escalationPolicyRepository;
    private final AppUserRepository appUserRepository;

    public EscalationPolicyController(
            EscalationPolicyRepository escalationPolicyRepository,
            AppUserRepository appUserRepository
    ) {
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public List<EscalationPolicyResponse> listPolicies() {
        return escalationPolicyRepository.findAll().stream()
                .map(EscalationPolicyResponse::from)
                .toList();
    }

    @PostMapping
    public EscalationPolicyResponse createPolicy(@Valid @RequestBody EscalationPolicyRequest request) {
        return EscalationPolicyResponse.from(escalationPolicyRepository.save(toPolicy(new EscalationPolicy(), request)));
    }

    @PutMapping("/{policyId}")
    public EscalationPolicyResponse updatePolicy(
            @PathVariable Long policyId,
            @Valid @RequestBody EscalationPolicyRequest request
    ) {
        EscalationPolicy policy = escalationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Escalation policy not found: " + policyId));

        return EscalationPolicyResponse.from(escalationPolicyRepository.save(toPolicy(policy, request)));
    }

    private EscalationPolicy toPolicy(EscalationPolicy policy, EscalationPolicyRequest request) {
        policy.setServiceName(request.serviceName());
        policy.setEnabled(request.enabled() == null || request.enabled());
        policy.setLevels(request.levels().stream()
                .sorted(Comparator.comparing(EscalationLevelRequest::levelNumber))
                .map(this::toLevel)
                .toList());
        return policy;
    }

    private EscalationLevel toLevel(EscalationLevelRequest request) {
        AppUser user = appUserRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));

        EscalationLevel level = new EscalationLevel();
        level.setLevelNumber(request.levelNumber());
        level.setUser(user);
        level.setWaitMinutes(request.waitMinutes());
        return level;
    }
}
