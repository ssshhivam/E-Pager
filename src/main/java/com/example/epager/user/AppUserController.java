package com.example.epager.user;

import com.example.epager.notification.UserDeviceService;
import com.example.epager.notification.dto.RegisterDeviceRequest;
import com.example.epager.notification.dto.UserDeviceResponse;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserRepository appUserRepository;
    private final UserDeviceService userDeviceService;
    private final PasswordEncoder passwordEncoder;

    public AppUserController(
            AppUserRepository appUserRepository,
            UserDeviceService userDeviceService,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.userDeviceService = userDeviceService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<AppUserResponse> listUsers() {
        return appUserRepository.findAll().stream()
                .map(AppUserResponse::from)
                .toList();
    }

    @PostMapping
    public AppUserResponse createUser(@Valid @RequestBody AppUserRequest request) {
        AppUser user = new AppUser();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(passwordEncoder.encode(
                request.password() == null || request.password().isBlank() ? "password" : request.password()
        ));
        user.setRole(request.role() == null ? AppRole.ENGINEER : request.role());
        return AppUserResponse.from(appUserRepository.save(user));
    }

    @GetMapping("/{userId}/devices")
    public List<UserDeviceResponse> listDevices(@PathVariable Long userId) {
        return userDeviceService.findActiveDevices(userId).stream()
                .map(UserDeviceResponse::from)
                .toList();
    }

    @PostMapping("/{userId}/devices")
    public UserDeviceResponse registerDevice(
            @PathVariable Long userId,
            @Valid @RequestBody RegisterDeviceRequest request
    ) {
        return UserDeviceResponse.from(userDeviceService.registerDevice(userId, request));
    }
}
