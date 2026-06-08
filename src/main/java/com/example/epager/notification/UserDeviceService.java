package com.example.epager.notification;

import com.example.epager.notification.dto.RegisterDeviceRequest;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserDeviceService {

    private final AppUserRepository appUserRepository;
    private final UserDeviceRepository userDeviceRepository;

    public UserDeviceService(AppUserRepository appUserRepository, UserDeviceRepository userDeviceRepository) {
        this.appUserRepository = appUserRepository;
        this.userDeviceRepository = userDeviceRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDevice> findActiveDevices(Long userId) {
        AppUser user = findUser(userId);
        return userDeviceRepository.findByUserAndActiveTrue(user);
    }

    @Transactional
    public UserDevice registerDevice(Long userId, RegisterDeviceRequest request) {
        AppUser user = findUser(userId);
        UserDevice device = new UserDevice();
        device.setUser(user);
        device.setPlatform(request.platform());
        device.setPushToken(request.pushToken());
        device.setDeviceName(request.deviceName());
        device.setActive(true);
        device.setCreatedAt(LocalDateTime.now());
        device.setLastSeenAt(LocalDateTime.now());
        return userDeviceRepository.save(device);
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}
