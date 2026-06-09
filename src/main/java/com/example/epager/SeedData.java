package com.example.epager;

import com.example.epager.escalation.EscalationLevel;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    public SeedData(
            AppUserRepository appUserRepository,
            EscalationPolicyRepository escalationPolicyRepository,
            UserDeviceRepository userDeviceRepository,
            ProjectRepository projectRepository,
            SupportGroupRepository supportGroupRepository,
            SupportGroupMemberRepository supportGroupMemberRepository,
            WebhookSecurityService webhookSecurityService,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.projectRepository = projectRepository;
        this.supportGroupRepository = supportGroupRepository;
        this.supportGroupMemberRepository = supportGroupMemberRepository;
        this.webhookSecurityService = webhookSecurityService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedWebhookSources();

        if (appUserRepository.count() > 0) {
            ensureExistingUsersHaveSecurityFields();
            seedSecurityUsers();
            return;
        }

        createUser("E-Pager Admin", "admin@epager.local", "+10000000000", AppRole.ADMIN);
        AppUser engineer = createUser("Shivam Engineer", "shivam.engineer@example.com", "+10000000001", AppRole.ENGINEER);
        AppUser lead = createUser("Ravi Lead", "ravi.lead@example.com", "+10000000002", AppRole.MANAGER);
        AppUser manager = createUser("Manish Manager", "manish.manager@example.com", "+10000000003", AppRole.MANAGER);

        createDevice(engineer, DevicePlatform.WEB, "demo-web-token-shivam", "Shivam browser");
        createDevice(lead, DevicePlatform.ANDROID, "demo-android-token-ravi", "Ravi Android");
        createDevice(manager, DevicePlatform.IOS, "demo-ios-token-manish", "Manish iPhone");

        Project project = createProject("payments", "Payments Project", "Seed project for payment alerts");
        SupportGroup supportGroup = createGroup(project, "primary-support", "Primary Support");
        addMember(supportGroup, engineer);
        addMember(supportGroup, lead);
        addMember(supportGroup, manager);

        EscalationPolicy policy = new EscalationPolicy();
        policy.setProjectKey("payments");
        policy.setGroupKey("primary-support");
        policy.setServiceName("payments");
        policy.setEnabled(true);
        policy.addLevel(createLevel(1, engineer, 5));
        policy.addLevel(createLevel(2, lead, 10));
        policy.addLevel(createLevel(3, manager, 15));
        escalationPolicyRepository.save(policy);
    }

    private void seedSecurityUsers() {
        appUserRepository.findByEmailIgnoreCase("admin@epager.local")
                .orElseGet(() -> createUser("E-Pager Admin", "admin@epager.local", "+10000000000", AppRole.ADMIN));
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

    private Project createProject(String projectKey, String name, String description) {
        Project project = new Project();
        project.setProjectKey(projectKey);
        project.setName(name);
        project.setDescription(description);
        project.setActive(true);
        project.setCreatedAt(LocalDateTime.now());
        return projectRepository.save(project);
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

    private void addMember(SupportGroup group, AppUser user) {
        SupportGroupMember member = new SupportGroupMember();
        member.setSupportGroup(group);
        member.setUser(user);
        member.setActive(true);
        member.setCreatedAt(LocalDateTime.now());
        supportGroupMemberRepository.save(member);
    }

    private EscalationLevel createLevel(int levelNumber, AppUser user, int waitMinutes) {
        EscalationLevel level = new EscalationLevel();
        level.setLevelNumber(levelNumber);
        level.setUser(user);
        level.setWaitMinutes(waitMinutes);
        return level;
    }
}
