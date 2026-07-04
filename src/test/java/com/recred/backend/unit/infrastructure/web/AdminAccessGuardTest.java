package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.AdminAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccessGuardTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AdminAccessGuard adminAccessGuard;

    @Mock
    private Authentication authentication;

    private Usuario adminUser;
    private Usuario regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new Usuario();
        adminUser.setRol(RolUsuario.ADMIN);

        regularUser = new Usuario();
        regularUser.setRol(RolUsuario.PADRE);
    }

    @Test
    void requireAdminAccess_ShouldPass_WhenUserIsAdmin() {
        when(authentication.getName()).thenReturn("admin-cognito-id");
        when(usuarioRepository.findByIdCognito("admin-cognito-id"))
                .thenReturn(Optional.of(adminUser));

        assertDoesNotThrow(() -> adminAccessGuard.requireAdminAccess(authentication));
    }

    @Test
    void requireAdminAccess_ShouldThrowAccessDeniedException_WhenUserIsNotAdmin() {
        when(authentication.getName()).thenReturn("user-cognito-id");
        when(usuarioRepository.findByIdCognito("user-cognito-id"))
                .thenReturn(Optional.of(regularUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> adminAccessGuard.requireAdminAccess(authentication));

        assertEquals("No tiene permisos de administrador", exception.getMessage());
    }

    @Test
    void requireAdminAccess_ShouldThrowException_WhenAuthenticationIsNull() {
        AuthenticationCredentialsNotFoundException exception = 
                assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> adminAccessGuard.requireAdminAccess(null));

        assertEquals("Usuario no autenticado", exception.getMessage());
    }

    @Test
    void requireAdminAccess_ShouldThrowException_WhenUserNotFoundInDb() {
        when(authentication.getName()).thenReturn("unknown-id");
        when(usuarioRepository.findByIdCognito("unknown-id")).thenReturn(Optional.empty());

        AuthenticationCredentialsNotFoundException exception = 
                assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> adminAccessGuard.requireAdminAccess(authentication));

        assertEquals("Usuario autenticado no sincronizado", exception.getMessage());
    }
}
