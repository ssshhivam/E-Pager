package com.example.epager;

import com.example.epager.escalation.EscalationLevel;
import com.example.epager.escalation.EscalationPolicy;
import com.example.epager.escalation.EscalationPolicyRepository;
import com.example.epager.notification.DevicePlatform;
import com.example.epager.notification.UserDevice;
import com.example.epager.notification.UserDeviceRepository;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SeedData implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final EscalationPolicyRepository escalationPolicyRepository;
    private final UserDeviceRepository userDeviceRepository;

    public SeedData(
            AppUserRepository appUserRepository,
            EscalationPolicyRepository escalationPolicyRepository,
            UserDeviceRepository userDeviceRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.escalationPolicyRepository = escalationPolicyRepository;
        this.userDeviceRepository = userDeviceRepository;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }

        AppUser engineer = createUser("Shivam Engineer", "shivam.engineer@example.com", "+10000000001");
        AppUser lead = createUser("Ravi Lead", "ravi.lead@example.com", "+10000000002");
        AppUser manager = createUser("Manish Manager", "manish.manager@example.com", "+10000000003");

        createDevice(engineer, DevicePlatform.WEB, "demo-web-token-shivam", "Shivam browser");
        createDevice(lead, DevicePlatform.ANDROID, "demo-android-token-ravi", "Ravi Android");
        createDevice(manager, DevicePlatform.IOS, "demo-ios-token-manish", "Manish iPhone");

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

    private EscalationLevel createLevel(int levelNumber, AppUser user, int waitMinutes) {
        EscalationLevel level = new EscalationLevel();
        level.setLevelNumber(levelNumber);
        level.setUser(user);
        level.setWaitMinutes(waitMinutes);
        return level;
    }
}
