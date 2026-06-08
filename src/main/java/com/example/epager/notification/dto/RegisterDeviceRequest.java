package com.example.epager.notification.dto;

import com.example.epager.notification.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceRequest(
        @NotNull DevicePlatform platform,
        @NotBlank String pushToken,
        String deviceName
) {
}
