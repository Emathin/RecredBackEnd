package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetTutorGlobalDashboard;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorGlobalDashboardController {

    private final GetTutorGlobalDashboard getTutorGlobalDashboard;

    public TutorGlobalDashboardController(GetTutorGlobalDashboard getTutorGlobalDashboard) {
        this.getTutorGlobalDashboard = getTutorGlobalDashboard;
    }

    @GetMapping("/me/dashboard-global")
    public ResponseEntity<TutorGlobalDashboardDTO> getGlobalDashboard(
            Authentication authentication) {
        TutorGlobalDashboardDTO dashboard = getTutorGlobalDashboard
                .executeByCognitoSubject(cognitoSubject(authentication));
        return ResponseEntity.ok(dashboard);
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
