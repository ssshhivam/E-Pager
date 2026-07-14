package com.example.epager;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.epager.escalation.EscalationLevel;
import com.example.epager.escalation.EscalationLevelUser;
import com.example.epager.escalation.EscalationLevelUserRepository;
import com.example.epager.escalation.EscalationPolicy;
import com.example.epager.escalation.EscalationPolicyRepository;
import com.example.epager.notification.DevicePlatform;
import com.example.epager.notification.UserDevice;
import com.example.epager.notification.UserDeviceRepository;
import com.example.epager.project.Project;
import com.example.epager.project.ProjectRepository;
import com.example.epager.project.SupportGroup;
import com.example.epager.project.SupportGroupMember;
import com.example.epager.project.SupportGroupMemberRepository;
import com.example.epager.project.SupportGroupRepository;
import com.example.epager.user.AppRole;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import com.example.epager.webhook.WebhookSecurityService;

@Component
public class SeedData implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final EscalationPolicyRepository escalationPolicyRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final ProjectRepository projectRepository;
    private final SupportGroupRepository supportGroupRepository;
    private final SupportGroupMemberRepository supportGroupMemberRepository;
    private final WebhookSecurityService webhookSecurityService;
    private final PasswordEncoder passwordEncoder;
    private final EscalationLevelUserRepository escalationLevelUserRepository;

    public SeedData(
            AppUserRepository appUserRepository,
            EscalationPolicyRepository escalationPolicyRepository,
            UserDeviceRepository userDeviceRepository,
            ProjectRepository projectRepository,
            SupportGroupRepository supportGroupRepository,
            SupportGroupMemberRepository supportGroupMemberRepository,
            WebhookSecurityService webhookSecurityService,
            PasswordEncoder passwordEncoder,
            EscalationLevelUserRepository escalationLevelUserRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.projectRepository = projectRepository;
        this.supportGroupRepository = supportGroupRepository;
        this.supportGroupMemberRepository = supportGroupMemberRepository;
        this.webhookSecurityService = webhookSecurityService;
        this.passwordEncoder = passwordEncoder;
        this.escalationLevelUserRepository = escalationLevelUserRepository;
    }

    @Override
    public void run(String... args) {
        seedWebhookSources();
        ensureExistingUsersHaveSecurityFields();

        ensureUser("E-Pager Admin", "admin@epager.local", "+10000000000", AppRole.ADMIN);
        AppUser engineer = ensureUser("Shivam Engineer", "shivam.engineer@example.com", "+10000000001", AppRole.ENGINEER);
        AppUser engineer2 = ensureUser("Raj Engineer", "raj.engineer@example.com", "+10000000004", AppRole.ENGINEER);
        AppUser lead = ensureUser("Ravi Lead", "ravi.lead@example.com", "+10000000002", AppRole.MANAGER);
        AppUser manager = ensureUser("Manish Manager", "manish.manager@example.com", "+10000000003", AppRole.MANAGER);

        ensureDevice(engineer, DevicePlatform.WEB, "demo-web-token-shivam", "Shivam browser");
        ensureDevice(engineer2, DevicePlatform.WEB, "demo-web-token-raj", "Raj browser");
        ensureDevice(lead, DevicePlatform.ANDROID, "demo-android-token-ravi", "Ravi Android");
        ensureDevice(manager, DevicePlatform.IOS, "demo-ios-token-manish", "Manish iPhone");

        Project project = ensureProject("payments", "Payments Project", "Seed project for payment alerts");
        SupportGroup supportGroup = ensureGroup(project, "primary-support", "Primary Support");
        ensureMember(supportGroup, engineer);
        ensureMember(supportGroup, engineer2);
        ensureMember(supportGroup, lead);
        ensureMember(supportGroup, manager);

        ensureEscalationPolicy(engineer, lead, manager, engineer2);
    }

    private void ensureExistingUsersHaveSecurityFields() {
        appUserRepository.findAll().forEach(user -> {
            boolean changed = false;
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode("password"));
                changed = true;
            }
            if (user.getRole() == null) {
                user.setRole(roleForEmail(user.getEmail()));
                changed = true;
            }
            if (changed) {
                appUserRepository.save(user);
            }
        });
    }

    private void seedWebhookSources() {
        webhookSecurityService.createSource(
                "grafana",
                "demo-webhook-token",
                "Demo source for Grafana-style webhook payloads",
                true
        );
        webhookSecurityService.createSource(
                "dynatrace",
                "demo-webhook-token",
                "Demo source for Dynatrace-style webhook payloads",
                true
        );
    }

    private AppUser createUser(String name, String email, String phoneNumber, AppRole role) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        return appUserRepository.save(user);
    }

    private AppUser ensureUser(String name, String email, String phoneNumber, AppRole role) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseGet(AppUser::new);
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode("password"));
        }
        return appUserRepository.save(user);
    }

    private AppRole roleForEmail(String email) {
        if ("admin@epager.local".equalsIgnoreCase(email)) {
            return AppRole.ADMIN;
        }
        if ("ravi.lead@example.com".equalsIgnoreCase(email) || "manish.manager@example.com".equalsIgnoreCase(email)) {
            return AppRole.MANAGER;
        }
        return AppRole.ENGINEER;
    }

    private void createDevice(AppUser user, DevicePlatform platform, String pushToken, String deviceName) {
        UserDevice device = new UserDevice();
        device.setUser(user);
        device.setPlatform(platform);
        device.setPushToken(pushToken);
        device.setDeviceName(deviceName);
        device.setActive(true);
        device.setCreatedAt(LocalDateTime.now());
        device.setLastSeenAt(LocalDateTime.now());
        userDeviceRepository.save(device);
    }

    private void ensureDevice(AppUser user, DevicePlatform platform, String pushToken, String deviceName) {
        boolean alreadyExists = userDeviceRepository.findByUserAndActiveTrue(user).stream()
                .anyMatch(device -> pushToken.equals(device.getPushToken()));
        if (!alreadyExists) {
            createDevice(user, platform, pushToken, deviceName);
        }
    }

    private Project createProject(String projectKey, String name, String description) {
        Project project = new Project();
        project.setProjectKey(projectKey);
        project.setName(name);
        project.setDescription(description);
        project.setActive(true);
        project.setCreatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }

    private Project ensureProject(String projectKey, String name, String description) {
        return projectRepository.findByProjectKeyIgnoreCaseAndActiveTrue(projectKey)
                .orElseGet(() -> createProject(projectKey, name, description));
    }

    private SupportGroup createGroup(Project project, String groupKey, String name) {
        SupportGroup group = new SupportGroup();
        group.setProject(project);
        group.setGroupKey(groupKey);
        group.setName(name);
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        return supportGroupRepository.save(group);
    }

    private SupportGroup ensureGroup(Project project, String groupKey, String name) {
        return supportGroupRepository.findByProjectAndGroupKeyIgnoreCaseAndActiveTrue(project, groupKey)
                .orElseGet(() -> createGroup(project, groupKey, name));
    }

    private void addMember(SupportGroup group, AppUser user) {
        SupportGroupMember member = new SupportGroupMember();
        member.setSupportGroup(group);
        member.setUser(user);
        member.setActive(true);
        member.setCreatedAt(LocalDateTime.now());
        supportGroupMemberRepository.save(member);
    }

    private void ensureMember(SupportGroup group, AppUser user) {
        if (supportGroupMemberRepository.findBySupportGroupAndUserAndActiveTrue(group, user).isEmpty()) {
            addMember(group, user);
        }
    }

    private void ensureEscalationPolicy(AppUser engineer, AppUser lead, AppUser manager, AppUser engineer2) {
        escalationPolicyRepository.findByProjectKeyIgnoreCaseAndGroupKeyIgnoreCaseAndEnabledTrue("payments", "primary-support")
				.orElseGet(() -> {
					EscalationPolicy policy = new EscalationPolicy();
					policy.setProjectKey("payments");
					policy.setGroupKey("primary-support");
					policy.setServiceName("payments");
					policy.setEnabled(true);
					EscalationLevel level1 = createLevel(1, engineer, 5);
					EscalationLevel level2 = createLevel(2, lead, 10);
					EscalationLevel level3 = createLevel(3, manager, 15);
					policy.addLevel(level1);
					policy.addLevel(level2);
					policy.addLevel(level3);
					escalationPolicyRepository.save(policy);
					List<EscalationLevelUser> mappings = List.of(createMapping(level1, engineer), createMapping(level1, engineer2),
							createMapping(level2, lead), createMapping(level3, manager));

					escalationLevelUserRepository.saveAll(mappings);
					return policy;
				});
    }

    private EscalationLevel createLevel(int levelNumber, AppUser user, int waitMinutes) {
        EscalationLevel level = new EscalationLevel();
        level.setLevelNumber(levelNumber);
//        EscalationLevelUser mapping = new EscalationLevelUser();
//		mapping.setEscalationLevel(level);
//		mapping.setUser(user);
//		mapping.setActive(true);
//		escalationLevelUserRepository.save(mapping);
//        level.setUser(user);
        level.setWaitMinutes(waitMinutes);
        return level;
    }
    
    private EscalationLevelUser createMapping(EscalationLevel level, AppUser user) {
        EscalationLevelUser mapping = new EscalationLevelUser();
        mapping.setEscalationLevel(level);
        mapping.setUser(user);
        mapping.setActive(true);
        return mapping;
    }
}
