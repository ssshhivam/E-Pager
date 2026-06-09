package com.example.epager.security;

import com.example.epager.security.dto.LoginRequest;
import com.example.epager.security.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return new LoginResponse(
                "Bearer",
                jwtTokenService.createToken(user.id(), user.getUsername(), user.role()),
                user.id(),
                user.user().getName(),
                user.getUsername(),
                user.role()
        );
    }
}
