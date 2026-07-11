package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.recred.backend.infrastructure.web.SyncUserRequest;
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

    private static final String COGNITO_SUBJECT = "cognito-sub";

    private UsuarioService usuarioService;
    private UsuarioController controller;
    private ResponseEntity<Map<String, Object>> response;

    private final UUID buffetId = UUID.randomUUID();
    private final UUID kiosqueroId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID alumnoId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        controller = new UsuarioController(usuarioService);
    }

    @Test
    void givenSellerProfileWhenSyncingUserThenReturnsBuffetId() {
        givenSellerUser();

        whenSyncingUser(null);

        thenResponseContains("buffetId", buffetId);
        thenResponseContains("kiosqueroId", kiosqueroId);
        thenResponseContains("rol", "VENDEDOR");
        thenSyncUserWasDelegatedWithoutRequest();
    }

    @Test
    void givenProfileRequestWhenSyncingUserThenReturnsPendingRoleAndIds() {
        givenPendingUserWithStudentAndTutor();
        SyncUserRequest request = givenSyncUserRequest();

        whenSyncingUser(request);

        thenResponseContains("rol", "PENDIENTE");
        thenResponseContains("alumnoId", alumnoId);
        thenResponseContains("tutorId", tutorId);
    }

    @Test
    void givenValidRoleWhenAssigningRoleThenUpdatesRoleUsingJwtSubject() {
        givenUserWithRole(RolUsuario.PADRE);

        whenAssigningRole("PADRE");

        thenResponseContains("rol", "PADRE");
        thenRoleUpdateWasDelegated("PADRE");
    }

    private void givenSellerUser() {
        Usuario usuario = buildSellerUser();
        when(usuarioService.sincronizarUsuarioCognito(
                any(Jwt.class), isNull()))
                .thenReturn(usuario);
    }

    private void givenPendingUserWithStudentAndTutor() {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setEmail("familia@example.com");
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        usuario.setAlumno(alumno);
        usuario.setTutor(tutor);
        when(usuarioService.sincronizarUsuarioCognito(
                any(Jwt.class), any()))
                .thenReturn(usuario);
    }

    private SyncUserRequest givenSyncUserRequest() {
        return new SyncUserRequest(
                "familia@example.com", "Ana", "Lopez", null, null);
    }

    private void givenUserWithRole(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setEmail("padre@example.com");
        usuario.setRol(rol);
        when(usuarioService.actualizarRol(
                COGNITO_SUBJECT, rol.name()))
                .thenReturn(usuario);
    }

    private void whenSyncingUser(SyncUserRequest request) {
        response = controller.syncUser(jwt(), request);
    }

    private void whenAssigningRole(String rol) {
        response = controller.assignRole(jwt(), Map.of("rol", rol));
    }

    private void thenResponseContains(String key, Object expected) {
        assertEquals(expected, response.getBody().get(key));
    }

    private void thenSyncUserWasDelegatedWithoutRequest() {
        verify(usuarioService).sincronizarUsuarioCognito(
                any(Jwt.class), isNull());
    }

    private void thenRoleUpdateWasDelegated(String rol) {
        verify(usuarioService).actualizarRol(COGNITO_SUBJECT, rol);
    }

    private Usuario buildSellerUser() {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);

        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(kiosqueroId);
        kiosquero.setBuffet(buffet);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
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
                .subject(COGNITO_SUBJECT)
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
    }
}

