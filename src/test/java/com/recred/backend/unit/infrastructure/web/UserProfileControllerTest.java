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
import org.springframework.http.ResponseEntity;
import com.recred.backend.infrastructure.web.UpdateUserProfileRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

import static com.recred.backend.domain.models.TipoPlan.AVANZADO;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private UserProfileController controller;
    private ResponseEntity<Map<String, Object>> userResponse;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        cloudinaryService = mock(CloudinaryService.class);
        suscripcionUsuarioRepository = mock(SuscripcionUsuarioRepository.class);
        controller = new UserProfileController(
                usuarioService,
                cloudinaryService,
                suscripcionUsuarioRepository,
                CLOCK
        );
    }

    @Test
    void givenCurrentUserWhenGettingMyUserThenReturnsMinimalSessionData() {
        givenCurrentUser();

        whenGetMyUser();

        thenReturnsMinimalSessionData();
        thenDelegatesToUserServiceToGetCurrentUser();
    }

    @Test
    void givenCurrentUserWhenGettingMyProfileThenReturnsFullProfileData() {
        givenCurrentUser();
        givenNoActiveSubscription();

        whenGetMyProfile();

        thenFullProfileDataIsReturned();
        thenFreePlanIsReturned();
        thenDelegatesToUserServiceToGetCurrentUser();
    }

    @Test
    void givenTrialUsedWhenGettingMyProfileThenReturnsHasUsedTrialTrue() {
        givenCurrentUser();
        givenNoActiveSubscription();
        givenTrialUsed(true);

        whenGetMyProfile();

        thenTrialStatusIsReturned(true);
    }

    @Test
    void givenActiveSubscriptionWhenGettingMyProfileThenReturnsPlanExpiration() {
        givenCurrentUserWithAdvancedPlan();
        givenActiveSubscription();

        whenGetMyProfile();

        thenPaidPlanExpirationIsReturned();
        thenDelegatesToUserServiceToGetCurrentUser();
        thenDelegatesToSubscriptionRepository();
    }

    @Test
    void givenValidUpdatePayloadWhenUpdatingMyProfileThenDelegatesToServiceAndReturnsUpdatedUser() {
        givenUpdateProfileDelegation();

        whenUpdateMyProfile();

        thenUpdatedProfileDataIsReturned();
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

    private void whenGetMyUser() {
        userResponse = controller.getMyUser(jwtAuth());
    }

    private void whenGetMyProfile() {
        userResponse = controller.getMyProfile(jwtAuth());
    }

    private void whenUpdateMyProfile() {
        userResponse = controller.updateMyProfile(jwtAuth(), validUpdatePayload());
    }

    private UpdateUserProfileRequest validUpdatePayload() {
        return new UpdateUserProfileRequest("Mateo", "Fortuna", "112233", "45238796");
    }

    private void thenReturnsMinimalSessionData() {
        assertEquals(200, userResponse.getStatusCode().value());
        assertEquals(USER_ID, userResponse.getBody().get("id"));
        assertEquals("mateo@example.com", userResponse.getBody().get("email"));
        assertEquals("Mateo", userResponse.getBody().get("firstName"));
        assertEquals("Fortuna", userResponse.getBody().get("lastName"));
        assertEquals("PADRE", userResponse.getBody().get("role"));
        assertFalse(userResponse.getBody().containsKey("phone"));
        assertFalse(userResponse.getBody().containsKey("documentNumber"));
    }

    private void thenFullProfileDataIsReturned() {
        assertEquals(200, userResponse.getStatusCode().value());
        assertEquals(USER_ID, userResponse.getBody().get("id"));
        assertEquals("mateo@example.com", userResponse.getBody().get("email"));
        assertEquals("Mateo", userResponse.getBody().get("firstName"));
        assertEquals("Fortuna", userResponse.getBody().get("lastName"));
        assertEquals("112233", userResponse.getBody().get("phone"));
        assertEquals("45238796", userResponse.getBody().get("documentNumber"));
        assertEquals("PADRE", userResponse.getBody().get("role"));
        assertEquals(false, userResponse.getBody().get("hasUsedTrial"));
    }

    private void thenUpdatedProfileDataIsReturned() {
        assertEquals(200, userResponse.getStatusCode().value());
        assertEquals(USER_ID, userResponse.getBody().get("id"));
        assertEquals("Mateo", userResponse.getBody().get("firstName"));
        assertEquals("Fortuna", userResponse.getBody().get("lastName"));
        assertEquals("112233", userResponse.getBody().get("phone"));
        assertEquals("45238796", userResponse.getBody().get("documentNumber"));
    }

    private void thenFreePlanIsReturned() {
        assertEquals("GRATUITO", userResponse.getBody().get("plan"));
        assertNull(userResponse.getBody().get("fechaVencimientoPlan"));
    }

    private void thenPaidPlanExpirationIsReturned() {
        assertEquals(200, userResponse.getStatusCode().value());
        assertEquals("AVANZADO", userResponse.getBody().get("plan"));
        assertEquals(LocalDateTime.parse("2026-08-06T10:30:00"), userResponse.getBody().get("fechaVencimientoPlan"));
    }

    private void thenTrialStatusIsReturned(boolean status) {
        assertEquals(200, userResponse.getStatusCode().value());
        assertEquals(status, userResponse.getBody().get("hasUsedTrial"));
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

    private JwtAuthenticationToken jwtAuth() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(COGNITO_SUBJECT)
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
