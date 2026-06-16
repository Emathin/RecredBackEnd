package com.recred.backend.web;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.recred.backend.infrastructure.web.UsuarioController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioControllerTest {

    private UsuarioService usuarioService;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        controller = new UsuarioController(usuarioService);
    }

    @Test
    void syncUserReturnsBuffetIdForSellerProfile() {
        UUID buffetId = UUID.randomUUID();
        Usuario usuario = sellerUser(buffetId);
        when(usuarioService.sincronizarUsuarioCognito(any(Jwt.class), isNull()))
                .thenReturn(usuario);

        ResponseEntity<Map<String, Object>> response = controller.syncUser(jwt(), null);

        assertEquals(buffetId, response.getBody().get("buffetId"));
        assertEquals(usuario.getKiosquero().getId(), response.getBody().get("kiosqueroId"));
        assertEquals("VENDEDOR", response.getBody().get("rol"));
        verify(usuarioService).sincronizarUsuarioCognito(any(Jwt.class), isNull());
    }

    private Usuario sellerUser(UUID buffetId) {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);

        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(UUID.randomUUID());
        kiosquero.setBuffet(buffet);

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("vendedor@example.com");
        usuario.setNombre("Vendedor");
        usuario.setApellido("Buffet");
        usuario.setRol(RolUsuario.VENDEDOR);
        usuario.setKiosquero(kiosquero);
        return usuario;
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
    }
}
