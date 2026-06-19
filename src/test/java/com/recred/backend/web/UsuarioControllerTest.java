package com.recred.backend.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UserService;
import com.recred.backend.infrastructure.web.SyncUserRequest;
import com.recred.backend.infrastructure.web.UsuarioController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioControllerTest {

    private UserService userService;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UsuarioController(userService);
    }

    @Test
    void syncUserReturnsBuffetIdForSellerProfile() {
        UUID buffetId = UUID.randomUUID();
        Usuario usuario = sellerUser(buffetId);
        when(userService.syncCognitoUser(any(Jwt.class), isNull()))
                .thenReturn(usuario);

        ResponseEntity<Map<String, Object>> response = controller.syncUser(jwt(), null);

        assertEquals(buffetId, response.getBody().get("buffetId"));
        assertEquals(usuario.getKiosquero().getId(), response.getBody().get("kiosqueroId"));
        assertEquals("VENDEDOR", response.getBody().get("rol"));
        verify(userService).syncCognitoUser(any(Jwt.class), isNull());
    }

    @Test
    void syncUserWithProfileRequestReturnsPendingRoleAndStudentTutorIds() {
        UUID alumnoId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("familia@example.com");
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        usuario.setAlumno(alumno);
        usuario.setTutor(tutor);
        SyncUserRequest request = new SyncUserRequest(
                "familia@example.com",
                "Ana",
                "Lopez",
                null,
                null
        );
        when(userService.syncCognitoUser(any(Jwt.class), any()))
                .thenReturn(usuario);

        ResponseEntity<Map<String, Object>> response = controller.syncUser(jwt(), request);

        assertEquals("PENDIENTE", response.getBody().get("rol"));
        assertEquals(alumnoId, response.getBody().get("alumnoId"));
        assertEquals(tutorId, response.getBody().get("tutorId"));
    }

    @Test
    void assignRoleShouldUpdateRoleUsingJwtSubject() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("padre@example.com");
        usuario.setRol(RolUsuario.PADRE);
        when(userService.updateRole("cognito-sub", "PADRE")).thenReturn(usuario);

        ResponseEntity<Map<String, Object>> response =
                controller.assignRole(jwt(), Map.of("rol", "PADRE"));

        assertEquals("PADRE", response.getBody().get("rol"));
        verify(userService).updateRole("cognito-sub", "PADRE");
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
