package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.SchoolLicenseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolLicenseAccessGuardTest {

    private static final String COGNITO_SUBJECT = "cognito-subject";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-06T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private KiosqueroRepository kiosqueroRepository;
    @Mock
    private SuscripcionColegioRepository suscripcionColegioRepository;

    private SchoolLicenseAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new SchoolLicenseAccessGuard(
                usuarioRepository,
                alumnoRepository,
                kiosqueroRepository,
                suscripcionColegioRepository,
                CLOCK
        );
    }

    @Test
    void requireInstitutionalAccessForAuthenticatedUser_WhenDirectorHasActiveLicense_ShouldAllow() {
        UUID schoolId = UUID.randomUUID();
        givenAuthenticatedUser(director(schoolId));
        givenActiveLicense(schoolId);

        assertDoesNotThrow(this::whenRequiringInstitutionalAccess);
    }

    @Test
    void requireInstitutionalAccessForAuthenticatedUser_WhenVendorSchoolHasNoLicense_ShouldDeny() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        givenAuthenticatedUser(vendor(userId));
        givenVendorSchool(userId, schoolId);
        givenNoLicense(schoolId);

        assertThrows(AccessDeniedException.class, this::whenRequiringInstitutionalAccess);
    }

    @Test
    void requireInstitutionalAccessForAuthenticatedUser_WhenParentHasNoLicense_ShouldAllow() {
        givenAuthenticatedUser(parent());

        assertDoesNotThrow(this::whenRequiringInstitutionalAccess);

        verify(suscripcionColegioRepository, never()).findActiveByColegioId(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void requireActiveSchoolForStudent_WhenSchoolIsInGrace_ShouldAllow() {
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        givenStudentSchool(studentId, schoolId);
        givenGraceLicense(schoolId);

        assertDoesNotThrow(() -> whenRequiringStudentAccess(studentId));
    }

    @Test
    void requireActiveSchoolForStudent_WhenSchoolHasNoLicense_ShouldDeny() {
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        givenStudentSchool(studentId, schoolId);
        givenNoLicense(schoolId);

        assertThrows(AccessDeniedException.class,
                () -> whenRequiringStudentAccess(studentId));
    }

    private void givenAuthenticatedUser(Usuario usuario) {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(usuario));
    }

    private void givenActiveLicense(UUID schoolId) {
        when(suscripcionColegioRepository.findActiveByColegioId(schoolId))
                .thenReturn(Optional.of(subscription(
                        LocalDateTime.of(2026, 7, 1, 12, 0),
                        LocalDateTime.of(2026, 8, 1, 12, 0),
                        LocalDateTime.of(2026, 9, 1, 12, 0)
                )));
    }

    private void givenGraceLicense(UUID schoolId) {
        when(suscripcionColegioRepository.findActiveByColegioId(schoolId))
                .thenReturn(Optional.of(subscription(
                        LocalDateTime.of(2026, 6, 1, 12, 0),
                        LocalDateTime.of(2026, 7, 1, 12, 0),
                        LocalDateTime.of(2026, 8, 1, 12, 0)
                )));
    }

    private void givenNoLicense(UUID schoolId) {
        when(suscripcionColegioRepository.findActiveByColegioId(schoolId))
                .thenReturn(Optional.empty());
    }

    private void givenVendorSchool(UUID userId, UUID schoolId) {
        when(kiosqueroRepository.findSchoolIdByUsuarioId(userId))
                .thenReturn(Optional.of(schoolId));
    }

    private void givenStudentSchool(UUID studentId, UUID schoolId) {
        when(alumnoRepository.findSchoolIdByStudentId(studentId))
                .thenReturn(Optional.of(schoolId));
    }

    private void whenRequiringInstitutionalAccess() {
        guard.requireInstitutionalAccessForAuthenticatedUser(authentication());
    }

    private void whenRequiringStudentAccess(UUID studentId) {
        guard.requireActiveSchoolForStudent(studentId);
    }

    private Usuario director(UUID schoolId) {
        Usuario usuario = user(RolUsuario.DIRECTIVO_COLEGIO);
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        usuario.setColegio(colegio);
        return usuario;
    }

    private Usuario vendor(UUID userId) {
        Usuario usuario = user(RolUsuario.VENDEDOR);
        usuario.setId(userId);
        return usuario;
    }

    private Usuario parent() {
        return user(RolUsuario.PADRE);
    }

    private Usuario user(RolUsuario role) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setRol(role);
        return usuario;
    }

    private SuscripcionColegio subscription(
            LocalDateTime start,
            LocalDateTime expiration,
            LocalDateTime graceUntil
    ) {
        SuscripcionColegio subscription = new SuscripcionColegio();
        subscription.setPrecio(BigDecimal.valueOf(20));
        subscription.setMoneda("USD");
        subscription.setFechaInicio(start);
        subscription.setFechaVencimiento(expiration);
        subscription.setFechaGraciaHasta(graceUntil);
        subscription.setEstado(EstadoSuscripcion.ACTIVA);
        return subscription;
    }

    private Authentication authentication() {
        return new TestingAuthenticationToken(COGNITO_SUBJECT, null);
    }
}
