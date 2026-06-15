package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class KiosqueroAccessGuard {

    private final UsuarioRepository usuarioRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final BuffetRepository buffetRepository;

    public KiosqueroAccessGuard(
            UsuarioRepository usuarioRepository,
            KiosqueroRepository kiosqueroRepository,
            BuffetRepository buffetRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.buffetRepository = buffetRepository;
    }

    public void requireBuffetAccess(Authentication authentication, UUID buffetId) {
        Usuario usuario = authenticatedUser(authentication);

        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.VENDEDOR && ownsBuffet(usuario, buffetId)) {
            return;
        }

        if (usuario.getRol() == RolUsuario.DIRECTIVO_COLEGIO
                && belongsToUserSchool(usuario, buffetId)) {
            return;
        }

        throw new AccessDeniedException("No tiene permisos sobre el buffet " + buffetId);
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

    private boolean ownsBuffet(Usuario usuario, UUID buffetId) {
        UUID ownedBuffetId = kiosqueroRepository.findBuffetByUsuarioId(usuario.getId());
        return buffetId != null && buffetId.equals(ownedBuffetId);
    }

    private boolean belongsToUserSchool(Usuario usuario, UUID buffetId) {
        if (usuario.getColegio() == null || usuario.getColegio().getId() == null) {
            return false;
        }

        return buffetRepository.findById(buffetId)
                .map(Buffet::getColegio)
                .filter(colegio -> colegio.getId() != null)
                .map(colegio -> colegio.getId().equals(usuario.getColegio().getId()))
                .orElse(false);
    }
}
