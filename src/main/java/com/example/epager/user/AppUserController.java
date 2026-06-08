package com.example.epager.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserRepository appUserRepository;

    public AppUserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
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
        return AppUserResponse.from(appUserRepository.save(user));
    }
}
