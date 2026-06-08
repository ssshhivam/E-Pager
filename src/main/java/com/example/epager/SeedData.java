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
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import com.example.epager.webhook.WebhookSecurityService;
import org.springframework.boot.CommandLineRunner;
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

    public SeedData(
            AppUserRepository appUserRepository,
            EscalationPolicyRepository escalationPolicyRepository,
            UserDeviceRepository userDeviceRepository,
            ProjectRepository projectRepository,
            SupportGroupRepository supportGroupRepository,
            SupportGroupMemberRepository supportGroupMemberRepository,
            WebhookSecurityService webhookSecurityService
    ) {
        this.appUserRepository = appUserRepository;
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.projectRepository = projectRepository;
        this.supportGroupRepository = supportGroupRepository;
        this.supportGroupMemberRepository = supportGroupMemberRepository;
        this.webhookSecurityService = webhookSecurityService;
    }

    @Override
    public void run(String... args) {
        seedWebhookSources();

        if (appUserRepository.count() > 0) {
            return;
        }

        AppUser engineer = createUser("Shivam Engineer", "shivam.engineer@example.com", "+10000000001");
        AppUser lead = createUser("Ravi Lead", "ravi.lead@example.com", "+10000000002");
        AppUser manager = createUser("Manish Manager", "manish.manager@example.com", "+10000000003");

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

    private AppUser createUser(String name, String email, String phoneNumber) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        return appUserRepository.save(user);
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
