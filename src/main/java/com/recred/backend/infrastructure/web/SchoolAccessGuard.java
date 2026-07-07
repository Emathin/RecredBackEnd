package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.TimeSlotNotFoundException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SchoolAccessGuard {

    private final UsuarioRepository usuarioRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final BuffetRepository buffetRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;

    public SchoolAccessGuard(
            UsuarioRepository usuarioRepository,
            KiosqueroRepository kiosqueroRepository,
            BuffetRepository buffetRepository,
            FranjaHorariaRepository franjaHorariaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.buffetRepository = buffetRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
    }

    public void requireSchoolAccess(Authentication authentication, UUID schoolId) {
        Usuario usuario = authenticatedUser(authentication);

        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.DIRECTIVO_COLEGIO
                && belongsToUserSchool(usuario, schoolId)) {
            return;
        }

        if (usuario.getRol() == RolUsuario.VENDEDOR && ownsBuffetInSchool(usuario, schoolId)) {
            return;
        }

        throw new AccessDeniedException("No tiene permisos sobre el colegio " + schoolId);
    }

    public void requireSchoolManagementAccess(Authentication authentication, UUID schoolId) {
        Usuario usuario = authenticatedUser(authentication);

        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.DIRECTIVO_COLEGIO
                && belongsToUserSchool(usuario, schoolId)) {
            return;
        }

        throw new AccessDeniedException("No tiene permisos para administrar el colegio "
                + schoolId);
    }

    public void requireTimeSlotAccess(Authentication authentication, UUID slotId) {
        UUID schoolId = franjaHorariaRepository.findById(slotId)
                .map(com.recred.backend.domain.models.FranjaHoraria::getColegioId)
                .orElseThrow(() -> new TimeSlotNotFoundException(slotId));
        requireSchoolAccess(authentication, schoolId);
    }

    public void requireSchoolDirectorAccess(Authentication authentication, UUID schoolId) {
        Usuario usuario = authenticatedUser(authentication);

        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.DIRECTIVO_COLEGIO
                && belongsToUserSchool(usuario, schoolId)) {
            return;
        }

        throw new AccessDeniedException(
                "No tiene permisos de directivo sobre el colegio " + schoolId);
    }

    public void requireBuffetDirectorAccess(Authentication authentication, UUID buffetId) {
        UUID schoolId = buffetRepository.findById(buffetId)
                .map(Buffet::getColegio)
                .map(com.recred.backend.domain.models.Colegio::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Buffet no encontrado o sin colegio asociado"));
        requireSchoolDirectorAccess(authentication, schoolId);
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

    private boolean belongsToUserSchool(Usuario usuario, UUID schoolId) {
        return usuario.getColegio() != null
                && usuario.getColegio().getId() != null
                && usuario.getColegio().getId().equals(schoolId);
    }

    private boolean ownsBuffetInSchool(Usuario usuario, UUID schoolId) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuario.getId());
        if (buffetId == null) {
            return false;
        }

        return buffetRepository.findById(buffetId)
                .map(Buffet::getColegio)
                .filter(colegio -> colegio.getId() != null)
                .map(colegio -> colegio.getId().equals(schoolId))
                .orElse(false);
    }
}
