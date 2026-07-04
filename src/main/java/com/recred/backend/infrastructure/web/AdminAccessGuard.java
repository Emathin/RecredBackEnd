package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminAccessGuard {

    private final UsuarioRepository usuarioRepository;

    public AdminAccessGuard(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void requireAdminAccess(Authentication authentication) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() != RolUsuario.ADMIN) {
            throw new AccessDeniedException("No tiene permisos de administrador");
        }
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
