package com.recred.backend.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class SchoolLicenseFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/ping",
            "/actuator/health",
            "/api/v1/school-registrations",
            "/api/v1/niveles",
            "/api/v1/payments/webhook",
            "/api/v1/users/me",
            "/api/v1/users/me/profile",
            "/api/v1/colegios/me",
            "/api/v1/usuarios/sync",
            "/api/v1/usuarios/rol",
            "/api/v1/sugerencias-consumo/test-notificacion"
    );

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/api/v1/invitaciones/tutor",
            "/api/v1/payments/subscriptions",
            "/api/v1/dispositivos",
            "/api/v1/notifications"
    );

    private final SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    public SchoolLicenseFilter(SchoolLicenseAccessGuard schoolLicenseAccessGuard) {
        this.schoolLicenseAccessGuard = schoolLicenseAccessGuard;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || isExcludedPath(request.getRequestURI())
                || isExcludedPrefix(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!shouldCheck(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            schoolLicenseAccessGuard.requireInstitutionalAccessForAuthenticatedUser(
                    authentication);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
        } catch (AccessDeniedException exception) {
            response.sendError(HttpStatus.FORBIDDEN.value(), exception.getMessage());
        }
    }

    private boolean shouldCheck(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isExcludedPath(String requestUri) {
        return EXCLUDED_PATHS.contains(requestUri);
    }

    private boolean isExcludedPrefix(String requestUri) {
        return EXCLUDED_PREFIXES.stream()
                .anyMatch(requestUri::startsWith);
    }
}
