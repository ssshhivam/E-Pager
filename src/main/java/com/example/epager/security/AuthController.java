package com.example.epager.security;

import com.example.epager.security.dto.ChangePasswordRequest;
import com.example.epager.security.dto.LoginRequest;
import com.example.epager.security.dto.LoginResponse;
import com.example.epager.security.dto.RefreshTokenRequest;
import com.example.epager.user.AppUser;
import com.example.epager.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUser currentUser;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            CurrentUser currentUser,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.currentUser = currentUser;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return issueTokens(user.user());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AppUser user = refreshTokenService.consumeAndRotate(request.refreshToken());
        return issueTokens(user);
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AppUser user = appUserRepository.findById(currentUser.require().id())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AccessDeniedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
        refreshTokenService.revokeActiveTokens(user);
    }

    private LoginResponse issueTokens(AppUser user) {
        JwtTokenService.TokenPair accessToken = jwtTokenService.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(
                "Bearer",
                accessToken.token(),
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.expiresAt(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
