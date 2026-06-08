package com.example.epager;

import com.example.epager.escalation.EscalationLevel;
import com.example.epager.escalation.EscalationPolicy;
import com.example.epager.escalation.EscalationPolicyRepository;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final EscalationPolicyRepository escalationPolicyRepository;

    public SeedData(AppUserRepository appUserRepository, EscalationPolicyRepository escalationPolicyRepository) {
        this.appUserRepository = appUserRepository;
        this.escalationPolicyRepository = escalationPolicyRepository;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }

        AppUser engineer = createUser("Shivam Engineer", "shivam.engineer@example.com", "+10000000001");
        AppUser lead = createUser("Ravi Lead", "ravi.lead@example.com", "+10000000002");
        AppUser manager = createUser("Manish Manager", "manish.manager@example.com", "+10000000003");

        EscalationPolicy policy = new EscalationPolicy();
        policy.setServiceName("payments");
        policy.setEnabled(true);
        policy.addLevel(createLevel(1, engineer, 5));
        policy.addLevel(createLevel(2, lead, 10));
        policy.addLevel(createLevel(3, manager, 15));
        escalationPolicyRepository.save(policy);
    }

    private AppUser createUser(String name, String email, String phoneNumber) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        return appUserRepository.save(user);
    }

    private EscalationLevel createLevel(int levelNumber, AppUser user, int waitMinutes) {
        EscalationLevel level = new EscalationLevel();
        level.setLevelNumber(levelNumber);
        level.setUser(user);
        level.setWaitMinutes(waitMinutes);
        return level;
    }
}
