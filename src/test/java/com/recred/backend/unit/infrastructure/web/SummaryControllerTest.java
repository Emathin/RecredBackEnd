package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetLastSummary;
import com.recred.backend.application.GetLastSummaryRequest;
import com.recred.backend.application.GetLastSummaryResponse;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
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
import static org.mockito.Mockito.when;

class SummaryControllerTest {

    private GetLastSummary getLastSummary;
    private UsuarioRepository usuarioRepository;
    private SummaryController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        getLastSummary = mock(GetLastSummary.class);
        usuarioRepository = mock(UsuarioRepository.class);
        authentication = mock(Authentication.class);
        controller = new SummaryController(getLastSummary, usuarioRepository);
    }

    @Test
    void getLastSummaryReturnsMappedSummaryWhenFound() {
        UUID userId = UUID.randomUUID();
        String cognitoId = "user123";
        Summary summary = summary(UUID.randomUUID());
        
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        
        when(authentication.getName()).thenReturn(cognitoId);
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.of(usuario));
        when(getLastSummary.execute(new GetLastSummaryRequest(userId)))
                .thenReturn(Optional.of(GetLastSummaryResponse.fromSummary(summary)));

        ResponseEntity<SummaryResponse> response = controller.getLastSummary(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(summary.getId().toString(), response.getBody().getId());
        assertEquals("01/06/26", response.getBody().getFechaDesde());
        assertEquals("Resumen semanal", response.getBody().getResumen());
    }

    @Test
    void getLastSummaryReturnsNotFoundWhenMissing() {
        UUID userId = UUID.randomUUID();
        String cognitoId = "user123";
        
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        
        when(authentication.getName()).thenReturn(cognitoId);
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.of(usuario));
        when(getLastSummary.execute(new GetLastSummaryRequest(userId))).thenReturn(Optional.empty());

        ResponseEntity<SummaryResponse> response = controller.getLastSummary(authentication);

        assertEquals(404, response.getStatusCode().value());
    }
    
    @Test
    void getLastSummaryThrowsWhenNotAuthenticated() {
        when(authentication.getName()).thenReturn(null);
        
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> 
            controller.getLastSummary(authentication)
        );
    }
    
    @Test
    void getLastSummaryThrowsWhenUserNotFound() {
        String cognitoId = "user123";
        when(authentication.getName()).thenReturn(cognitoId);
        when(usuarioRepository.findByIdCognito(cognitoId)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> 
            controller.getLastSummary(authentication)
        );
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

