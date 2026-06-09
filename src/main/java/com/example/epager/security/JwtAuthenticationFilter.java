package com.example.epager.security;

import com.example.epager.user.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AppUserRepository appUserRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, AppUserRepository appUserRepository) {
        this.jwtTokenService = jwtTokenService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtTokenService.validate(header.substring(7))
                    .flatMap(claims -> appUserRepository.findById(claims.userId()))
                    .map(AuthenticatedUser::new)
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
                    ));
        }
        filterChain.doFilter(request, response);
    }
}
