package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.EstadoLicenciaColegio;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class SchoolLicenseAccessGuard {

    private static final String BLOCKED_MESSAGE =
            "El colegio no tiene licencia activa o en periodo de gracia";

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final SuscripcionColegioRepository suscripcionColegioRepository;
    private final Clock clock;

    public SchoolLicenseAccessGuard(
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository,
            KiosqueroRepository kiosqueroRepository,
            SuscripcionColegioRepository suscripcionColegioRepository,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.suscripcionColegioRepository = suscripcionColegioRepository;
        this.clock = clock;
    }

    public void requireInstitutionalAccessForAuthenticatedUser(Authentication authentication) {
        Usuario usuario = authenticatedUser(authentication);
        if (shouldSkipInstitutionalCheck(usuario)) {
            return;
        }

        UUID schoolId = schoolIdFor(usuario)
                .orElseThrow(() -> new AccessDeniedException(
                        "No se pudo resolver el colegio asociado al usuario"));
        requireActiveSchoolById(schoolId);
    }

    public void requireActiveSchoolForStudent(UUID studentId) {
        UUID schoolId = schoolIdByStudent(studentId);
        requireActiveSchoolById(schoolId);
    }

    public void requireActiveSchoolForStudents(Collection<UUID> studentIds) {
        if (studentIds == null) {
            return;
        }

        studentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::requireActiveSchoolForStudent);
    }

    public boolean isAuthenticatedParent(Authentication authentication) {
        return authenticatedUser(authentication).getRol() == RolUsuario.PADRE;
    }

    public EstadoLicenciaColegio getLicenseStatusForUser(Usuario usuario) {
        if (shouldSkipInstitutionalCheck(usuario)) {
            return EstadoLicenciaColegio.ACTIVA;
        }

        return schoolIdFor(usuario)
                .flatMap(suscripcionColegioRepository::findActiveByColegioId)
                .map(sub -> sub.estadoLicenciaAt(now()))
                .orElse(EstadoLicenciaColegio.SIN_LICENCIA);
    }

    public LocalDateTime getLicenseExpirationForUser(Usuario usuario) {
        if (shouldSkipInstitutionalCheck(usuario)) {
            return null;
        }

        return schoolIdFor(usuario)
                .flatMap(suscripcionColegioRepository::findActiveByColegioId)
                .map(SuscripcionColegio::getFechaVencimiento)
                .orElse(null);
    }

    private boolean shouldSkipInstitutionalCheck(Usuario usuario) {
        return usuario.getRol() == RolUsuario.ADMIN
                || usuario.getRol() == RolUsuario.PADRE;
    }

    private Optional<UUID> schoolIdFor(Usuario usuario) {
        if (usuario.getRol() == RolUsuario.DIRECTIVO_COLEGIO) {
            return Optional.ofNullable(usuario.getColegio())
                    .map(Colegio::getId);
        }

        if (usuario.getRol() == RolUsuario.ALUMNO) {
            return Optional.ofNullable(usuario.getAlumno())
                    .map(Alumno::getId)
                    .flatMap(alumnoRepository::findSchoolIdByStudentId);
        }

        if (usuario.getRol() == RolUsuario.VENDEDOR) {
            return kiosqueroRepository.findSchoolIdByUsuarioId(usuario.getId());
        }

        return Optional.empty();
    }

    private UUID schoolIdByStudent(UUID studentId) {
        if (studentId == null) {
            throw new AccessDeniedException("El alumno es requerido para validar la licencia");
        }

        return alumnoRepository.findSchoolIdByStudentId(studentId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No se pudo resolver el colegio asociado al alumno"));
    }

    private void requireActiveSchoolById(UUID schoolId) {
        if (!hasInstitutionalAccess(schoolId)) {
            throw new AccessDeniedException(BLOCKED_MESSAGE);
        }
    }

    private boolean hasInstitutionalAccess(UUID schoolId) {
        return suscripcionColegioRepository.findActiveByColegioId(schoolId)
                .map(this::canUseApp)
                .orElse(false);
    }

    private boolean canUseApp(SuscripcionColegio subscription) {
        return subscription.canUseAppAt(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Usuario authenticatedUser(Authentication authentication) {
        String subject = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
        return usuarioRepository.findByIdCognito(subject)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario autenticado no sincronizado"));
    }
}
