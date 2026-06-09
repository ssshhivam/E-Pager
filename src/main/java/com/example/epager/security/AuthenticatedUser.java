package com.example.epager.security;

import com.example.epager.user.AppRole;
import com.example.epager.user.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails {

    private final AppUser user;

    public AuthenticatedUser(AppUser user) {
        this.user = user;
    }

    public Long id() {
        return user.getId();
    }

    public AppRole role() {
        return user.getRole();
    }

    public AppUser user() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }
}
