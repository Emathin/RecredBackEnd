package com.recred.backend.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SchoolLicenseStudentInterceptor implements HandlerInterceptor {

    private static final Set<String> STUDENT_ID_KEYS = Set.of(
            "alumnoId",
            "studentId",
            "idAlumno",
            "idStudent"
    );

    private final SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    public SchoolLicenseStudentInterceptor(SchoolLicenseAccessGuard schoolLicenseAccessGuard) {
        this.schoolLicenseAccessGuard = schoolLicenseAccessGuard;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!shouldCheck(authentication)) {
            return true;
        }

        Set<UUID> studentIds = studentIds(request);
        if (studentIds.isEmpty()) {
            return true;
        }

        if (!schoolLicenseAccessGuard.isAuthenticatedParent(authentication)) {
            return true;
        }

        studentIds.forEach(schoolLicenseAccessGuard::requireActiveSchoolForStudent);
        return true;
    }

    private boolean shouldCheck(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Set<UUID> studentIds(HttpServletRequest request) {
        Set<UUID> studentIds = new LinkedHashSet<>();
        studentIds.addAll(pathStudentIds(request));
        studentIds.addAll(queryStudentIds(request));
        return studentIds;
    }

    private Set<UUID> pathStudentIds(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variables instanceof Map<?, ?> pathVariables)) {
            return Set.of();
        }

        Set<UUID> studentIds = new LinkedHashSet<>();
        pathVariables.forEach((key, value) -> {
            if (STUDENT_ID_KEYS.contains(String.valueOf(key))) {
                parseUuid(String.valueOf(value)).ifPresent(studentIds::add);
            }
        });
        return studentIds;
    }

    private Set<UUID> queryStudentIds(HttpServletRequest request) {
        Set<UUID> studentIds = new LinkedHashSet<>();
        STUDENT_ID_KEYS.forEach(key -> Optional.ofNullable(request.getParameterValues(key))
                .stream()
                .flatMap(Arrays::stream)
                .map(this::parseUuid)
                .flatMap(Optional::stream)
                .forEach(studentIds::add));
        return studentIds;
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
