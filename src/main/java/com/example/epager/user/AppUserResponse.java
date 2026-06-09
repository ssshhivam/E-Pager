package com.example.epager.user;

public record AppUserResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        AppRole role
) {
    public static AppUserResponse from(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole()
        );
    }
}
