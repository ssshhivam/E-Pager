package com.example.epager.notification.dto;

import com.example.epager.notification.DevicePlatform;
import com.example.epager.notification.UserDevice;

import java.time.LocalDateTime;

public record UserDeviceResponse(
        Long id,
        Long userId,
        DevicePlatform platform,
        String pushToken,
        String deviceName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastSeenAt
) {
    public static UserDeviceResponse from(UserDevice device) {
        return new UserDeviceResponse(
                device.getId(),
                device.getUser().getId(),
                device.getPlatform(),
                device.getPushToken(),
                device.getDeviceName(),
                device.isActive(),
                device.getCreatedAt(),
                device.getLastSeenAt()
        );
    }
}
