package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SuscripcionUsuario;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.TipoPlan.AVANZADO;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private static final String COGNITO_SUBJECT = "cognito-sub";
    private static final UUID USER_ID = randomUUID();
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
        givenCurrentUser();

        ResultActions response = whenGetMyUser();

        thenReturnsMinimalSessionData(response);
        thenDelegatesToUserServiceToGetCurrentUser();
    }

    @Test
    void getMyProfileReturnsFullProfileData() throws Exception {
        givenCurrentUser();
        givenNoActiveSubscription();

        ResultActions response = whenGetMyProfile();

        thenFullProfileDataIsReturned(response);
        thenFreePlanIsReturned(response);
        thenDelegatesToUserServiceToGetCurrentUser();
    }

    @Test
    void getMyProfileReturnsHasUsedTrialTrueWhenUserAlreadyUsedTrial() throws Exception {
        givenCurrentUser();
        givenNoActiveSubscription();
        givenTrialUsed(true);

        ResultActions response = whenGetMyProfile();

        thenTrialStatusIsReturned(response, true);
    }

    @Test
    void getMyProfileReturnsPlanExpirationWhenUserHasActiveSubscription() throws Exception {
        givenCurrentUserWithAdvancedPlan();
        givenActiveSubscription();

        ResultActions response = whenGetMyProfile();

        thenPaidPlanExpirationIsReturned(response);
        thenDelegatesToUserServiceToGetCurrentUser();
        thenDelegatesToSubscriptionRepository();
    }

    @Test
    void updateMyProfileDelegatesToServiceAndReturnsUpdatedUser() throws Exception {
        givenUpdateProfileDelegation();

        ResultActions response = whenUpdateMyProfile();

        thenUpdatedProfileDataIsReturned(response);
        thenUpdateProfileDelegatesToServiceWithCorrectData();
    }

    private void givenCurrentUser() {
        Usuario usuario = buildDefaultUser();
        when(usuarioService.obtenerUsuarioActual(COGNITO_SUBJECT)).thenReturn(usuario);
    }

    private void givenCurrentUserWithAdvancedPlan() {
        Usuario usuario = buildDefaultUser();
        usuario.setPlan(AVANZADO);
        when(usuarioService.obtenerUsuarioActual(COGNITO_SUBJECT)).thenReturn(usuario);
    }

    private void givenNoActiveSubscription() {
        when(suscripcionUsuarioRepository.findActiveByUsuarioId(USER_ID))
                .thenReturn(Optional.empty());
    }

    private void givenActiveSubscription() {
        LocalDateTime fechaVencimiento = LocalDateTime.of(2026, 8, 6, 10, 30);
        SuscripcionUsuario suscripcion = new SuscripcionUsuario();
        suscripcion.setFechaVencimiento(fechaVencimiento);
        when(suscripcionUsuarioRepository.findActiveByUsuarioId(USER_ID))
                .thenReturn(Optional.of(suscripcion));
    }

    private void givenTrialUsed(boolean used) {
        when(suscripcionUsuarioRepository.hasUsedTrial(USER_ID)).thenReturn(used);
    }

    private void givenUpdateProfileDelegation() {
        Usuario usuario = buildDefaultUser();
        when(usuarioService.updateProfile(
                eq(COGNITO_SUBJECT),
                eq("Mateo"),
                eq("Fortuna"),
                eq("112233"),
                eq("45238796")
        )).thenReturn(usuario);
    }

    private ResultActions whenGetMyUser() throws Exception {
        return mockMvc.perform(get("/api/v1/users/me")
                .principal(authentication()));
    }

    private ResultActions whenGetMyProfile() throws Exception {
        return mockMvc.perform(get("/api/v1/users/me/profile")
                .principal(authentication()));
    }

    private ResultActions whenUpdateMyProfile() throws Exception {
        return mockMvc.perform(patch("/api/v1/users/me/profile")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdatePayload()));
    }

    private String validUpdatePayload() {
        return """
                {
                  "firstName": "Mateo",
                  "lastName": "Fortuna",
                  "phone": "112233",
                  "documentNumber": "45238796"
                }
                """;
    }

    private void thenReturnsMinimalSessionData(ResultActions response) throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("mateo@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.role").value("PADRE"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.documentNumber").doesNotExist());
    }

    private void thenFullProfileDataIsReturned(ResultActions response) throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("mateo@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.phone").value("112233"))
                .andExpect(jsonPath("$.documentNumber").value("45238796"))
                .andExpect(jsonPath("$.role").value("PADRE"))
                .andExpect(jsonPath("$.hasUsedTrial").value(false));
    }

    private void thenUpdatedProfileDataIsReturned(ResultActions response) throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.phone").value("112233"))
                .andExpect(jsonPath("$.documentNumber").value("45238796"));
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

    private void thenTrialStatusIsReturned(ResultActions response, boolean status) throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUsedTrial").value(status));
    }

    private void thenDelegatesToUserServiceToGetCurrentUser() {
        verify(usuarioService).obtenerUsuarioActual(COGNITO_SUBJECT);
    }

    private void thenDelegatesToSubscriptionRepository() {
        verify(suscripcionUsuarioRepository).findActiveByUsuarioId(USER_ID);
    }

    private void thenUpdateProfileDelegatesToServiceWithCorrectData() {
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(usuarioService).updateProfile(
                subjectCaptor.capture(),
                eq("Mateo"),
                eq("Fortuna"),
                eq("112233"),
                eq("45238796")
        );
        assertEquals(COGNITO_SUBJECT, subjectCaptor.getValue());
    }

    private Usuario buildDefaultUser() {
        Usuario usuario = new Usuario();
        usuario.setId(USER_ID);
        usuario.setEmail("mateo@example.com");
        usuario.setNombre("Mateo");
        usuario.setApellido("Fortuna");
        usuario.setTelefono("112233");
        usuario.setDni("45238796");
        usuario.setRol(RolUsuario.PADRE);
        return usuario;
    }

    private Authentication authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(COGNITO_SUBJECT)
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
