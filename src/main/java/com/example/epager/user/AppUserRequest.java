package com.example.epager.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AppUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        String phoneNumber,
        String password,
        AppRole role
) {
}
