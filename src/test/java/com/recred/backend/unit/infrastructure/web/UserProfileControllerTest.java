package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import com.recred.backend.infrastructure.web.UserProfileController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    private UsuarioService usuarioService;
    private CloudinaryService cloudinaryService;
    private SuscripcionUsuarioRepository suscripcionUsuarioRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        cloudinaryService = mock(CloudinaryService.class);
        suscripcionUsuarioRepository = mock(SuscripcionUsuarioRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserProfileController(
                        usuarioService,
                        cloudinaryService,
                        suscripcionUsuarioRepository,
                        CLOCK
                ))
                .build();
    }

    @Test
    void getMyUserReturnsMinimalSessionData() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = usuario(userId);
        when(usuarioService.obtenerUsuarioActual("cognito-sub")).thenReturn(usuario);

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("mateo@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.role").value("PADRE"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.documentNumber").doesNotExist());

        verify(usuarioService).obtenerUsuarioActual("cognito-sub");
    }

    @Test
    void getMyProfileReturnsFullProfileData() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = usuario(userId);
        givenCurrentUser(usuario);
        when(suscripcionUsuarioRepository.findActiveByUsuarioId(userId)).thenReturn(Optional.empty());

        ResultActions response = whenGetMyProfile();

        thenFullProfileDataIsReturned(response, userId);
        thenFreePlanIsReturned(response);

        verify(usuarioService).obtenerUsuarioActual("cognito-sub");
    }

    @Test
    void getMyProfileReturnsPlanExpirationWhenUserHasActiveSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime fechaVencimiento = LocalDateTime.of(2026, 8, 6, 10, 30);
        Usuario usuario = usuario(userId);
        usuario.setPlan(TipoPlan.AVANZADO);
        SuscripcionUsuario suscripcion = suscripcion(fechaVencimiento);
        givenCurrentUser(usuario);
        givenActiveSubscription(userId, suscripcion);

        ResultActions response = whenGetMyProfile();

        thenPaidPlanExpirationIsReturned(response);

        verify(usuarioService).obtenerUsuarioActual("cognito-sub");
        verify(suscripcionUsuarioRepository).findActiveByUsuarioId(userId);
    }

    @Test
    void updateMyProfileDelegatesToServiceAndReturnsUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = usuario(userId);
        when(usuarioService.updateProfile(
                eq("cognito-sub"),
                eq("Mateo"),
                eq("Fortuna"),
                eq("112233"),
                eq("45238796")
        )).thenReturn(usuario);

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Mateo",
                                  "lastName": "Fortuna",
                                  "phone": "112233",
                                  "documentNumber": "45238796"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.phone").value("112233"))
                .andExpect(jsonPath("$.documentNumber").value("45238796"));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(usuarioService).updateProfile(
                subjectCaptor.capture(),
                eq("Mateo"),
                eq("Fortuna"),
                eq("112233"),
                eq("45238796")
        );
        assertEquals("cognito-sub", subjectCaptor.getValue());
    }

    private void givenCurrentUser(Usuario usuario) {
        when(usuarioService.obtenerUsuarioActual("cognito-sub")).thenReturn(usuario);
    }

    private void givenActiveSubscription(UUID userId, SuscripcionUsuario suscripcion) {
        when(suscripcionUsuarioRepository.findActiveByUsuarioId(userId))
                .thenReturn(Optional.of(suscripcion));
    }

    private ResultActions whenGetMyProfile() throws Exception {
        return mockMvc.perform(get("/api/v1/users/me/profile")
                .principal(authentication()));
    }

    private void thenFullProfileDataIsReturned(ResultActions response, UUID userId)
            throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("mateo@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.phone").value("112233"))
                .andExpect(jsonPath("$.documentNumber").value("45238796"))
                .andExpect(jsonPath("$.role").value("PADRE"));
    }

    private void thenFreePlanIsReturned(ResultActions response) throws Exception {
        response
                .andExpect(jsonPath("$.plan").value("GRATUITO"))
                .andExpect(jsonPath("$.fechaVencimientoPlan").value(nullValue()));
    }

    private void thenPaidPlanExpirationIsReturned(ResultActions response) throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("AVANZADO"))
                .andExpect(jsonPath("$.fechaVencimientoPlan").value("2026-08-06T10:30:00"));
    }

    private Usuario usuario(UUID userId) {
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail("mateo@example.com");
        usuario.setNombre("Mateo");
        usuario.setApellido("Fortuna");
        usuario.setTelefono("112233");
        usuario.setDni("45238796");
        usuario.setRol(RolUsuario.PADRE);
        return usuario;
    }

    private Usuario schoolUser(UUID userId, UUID schoolId) {
        Usuario usuario = usuario(userId);
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        usuario.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        usuario.setColegio(colegio);
        return usuario;
    }

    private SuscripcionUsuario suscripcion(LocalDateTime fechaVencimiento) {
        SuscripcionUsuario suscripcion = new SuscripcionUsuario();
        suscripcion.setFechaVencimiento(fechaVencimiento);
        return suscripcion;
    }

    private Authentication authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
