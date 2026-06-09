package com.example.epager.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        String phoneNumber,
        @NotBlank @Size(min = 8) String password,
        AppRole role
) {
}
