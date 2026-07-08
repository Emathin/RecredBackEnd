package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetLastSummary;
import com.recred.backend.application.GetLastSummaryRequest;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumen")
public class SummaryController {

    private final GetLastSummary getLastSummary;
    private final UsuarioRepository usuarioRepository;
    private final PlanAccessGuard planAccessGuard;

    public SummaryController(
                             GetLastSummary getLastSummary,
                             UsuarioRepository usuarioRepository,
                             PlanAccessGuard planAccessGuard) {
        this.getLastSummary = getLastSummary;
        this.usuarioRepository = usuarioRepository;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping("/me")
    public ResponseEntity<SummaryResponse> getLastSummary(Authentication authentication) {
        planAccessGuard.requireTutorIntermediate(authentication);
        String cognitoId = cognitoSubject(authentication);
        UUID userId = usuarioRepository.findByIdCognito(cognitoId)
                .map(Usuario::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return getLastSummary.execute(new GetLastSummaryRequest(userId))
                .map(SummaryResponse::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
