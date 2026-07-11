package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetLastSummary;
import com.recred.backend.application.GetLastSummaryRequest;
import com.recred.backend.application.GetLastSummaryResponse;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SummaryController;
import com.recred.backend.infrastructure.web.SummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummaryControllerTest {

    private GetLastSummary getLastSummary;
    private UsuarioRepository usuarioRepository;
    private PlanAccessGuard planAccessGuard;
    private SummaryController controller;
    private Authentication authentication;

    private final UUID userId = UUID.randomUUID();
    private final String cognitoId = "user123";
    private final Summary foundSummary = summary(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        getLastSummary = mock(GetLastSummary.class);
        usuarioRepository = mock(UsuarioRepository.class);
        planAccessGuard = mock(PlanAccessGuard.class);
        authentication = mock(Authentication.class);
        controller = new SummaryController(getLastSummary, usuarioRepository, planAccessGuard);
    }

    @Test
    void givenFoundSummaryWhenGettingLastSummaryThenReturnsMappedSummary() {
        givenAuthenticationReturnsCognitoId();
        givenUsuarioRepositoryReturnsUser();
        givenGetLastSummaryReturnsFoundSummary();

        ResponseEntity<SummaryResponse> response = whenGettingLastSummary();

        thenSummaryIsReturned(response);
        thenTutorIntermediateAccessWasRequired();
    }

    @Test
    void givenMissingSummaryWhenGettingLastSummaryThenReturnsNotFound() {
        givenAuthenticationReturnsCognitoId();
        givenUsuarioRepositoryReturnsUser();
        givenGetLastSummaryReturnsEmpty();

        ResponseEntity<SummaryResponse> response = whenGettingLastSummary();

        thenNotFoundIsReturned(response);
        thenTutorIntermediateAccessWasRequired();
    }
    
    @Test
    void givenUnauthenticatedUserWhenGettingLastSummaryThenThrowsException() {
        givenAuthenticationReturnsNull();
        
        assertThrows(AuthenticationCredentialsNotFoundException.class, this::whenGettingLastSummary);
    }
    
    @Test
    void givenUserNotFoundWhenGettingLastSummaryThenThrowsException() {
        givenAuthenticationReturnsCognitoId();
        givenUsuarioRepositoryReturnsEmpty();
        
        assertThrows(RuntimeException.class, this::whenGettingLastSummary);
    }

    private void givenAuthenticationReturnsCognitoId() {
        when(authentication.getName()).thenReturn(cognitoId);
    }

    private void givenAuthenticationReturnsNull() {
        when(authentication.getName()).thenReturn(null);
    }

    private void givenUsuarioRepositoryReturnsUser() {
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.of(usuario));
    }

    private void givenUsuarioRepositoryReturnsEmpty() {
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.empty());
    }

    private void givenGetLastSummaryReturnsFoundSummary() {
        when(getLastSummary.execute(new GetLastSummaryRequest(userId)))
                .thenReturn(Optional.of(GetLastSummaryResponse.fromSummary(foundSummary)));
    }

    private void givenGetLastSummaryReturnsEmpty() {
        when(getLastSummary.execute(new GetLastSummaryRequest(userId))).thenReturn(Optional.empty());
    }

    private ResponseEntity<SummaryResponse> whenGettingLastSummary() {
        return controller.getLastSummary(authentication);
    }

    private void thenSummaryIsReturned(ResponseEntity<SummaryResponse> response) {
        assertEquals(200, response.getStatusCode().value());
        assertEquals(foundSummary.getId().toString(), response.getBody().getId());
        assertEquals("01/06/26", response.getBody().getFechaDesde());
        assertEquals("Resumen semanal", response.getBody().getResumen());
    }

    private void thenNotFoundIsReturned(ResponseEntity<SummaryResponse> response) {
        assertEquals(404, response.getStatusCode().value());
    }

    private void thenTutorIntermediateAccessWasRequired() {
        verify(planAccessGuard).requireTutorIntermediate(authentication);
    }

    private Summary summary(UUID summaryId) {
        Summary summary = new Summary();
        summary.setId(summaryId);
        summary.setFechaDesde(LocalDateTime.of(2026, 6, 1, 0, 0));
        summary.setFechaHasta(LocalDateTime.of(2026, 6, 8, 0, 0));
        summary.setResumen("Resumen semanal");
        return summary;
    }
}
