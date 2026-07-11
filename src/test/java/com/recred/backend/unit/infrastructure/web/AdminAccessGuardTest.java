package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.AdminAccessGuard;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccessGuardTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminAccessGuard adminAccessGuard;

    private final String ADMIN_COGNITO_ID = "admin-cognito-id";
    private final String USER_COGNITO_ID = "user-cognito-id";
    private final String UNKNOWN_ID = "unknown-id";

    @Test
    void requireAdminAccessShouldPassWhenUserIsAdmin() {
        givenAdminUserAutenticated();
        
        assertDoesNotThrow(() -> whenRequireAdminAccess(authentication));
    }

    @Test
    void requireAdminAccessShouldThrowAccessDeniedExceptionWhenUserIsNotAdmin() {
        givenRegularUserAutenticated();
        
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> whenRequireAdminAccess(authentication));
                
        thenExceptionMessageIs("No tiene permisos de administrador", exception);
    }

    @Test
    void requireAdminAccessShouldThrowExceptionWhenAuthenticationIsNull() {
        AuthenticationCredentialsNotFoundException exception = 
                assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> whenRequireAdminAccess(null));
                
        thenExceptionMessageIs("Usuario no autenticado", exception);
    }

    @Test
    void requireAdminAccessShouldThrowExceptionWhenUserNotFoundInDb() {
        givenUnknownUserAutenticated();
        
        AuthenticationCredentialsNotFoundException exception = 
                assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> whenRequireAdminAccess(authentication));
                
        thenExceptionMessageIs("Usuario autenticado no sincronizado", exception);
    }

    private void givenAdminUserAutenticated() {
        Usuario adminUser = new Usuario();
        adminUser.setRol(RolUsuario.ADMIN);
        when(authentication.getName()).thenReturn(ADMIN_COGNITO_ID);
        when(usuarioRepository.findByIdCognito(ADMIN_COGNITO_ID))
                .thenReturn(Optional.of(adminUser));
    }

    private void givenRegularUserAutenticated() {
        Usuario regularUser = new Usuario();
        regularUser.setRol(RolUsuario.PADRE);
        when(authentication.getName()).thenReturn(USER_COGNITO_ID);
        when(usuarioRepository.findByIdCognito(USER_COGNITO_ID))
                .thenReturn(Optional.of(regularUser));
    }

    private void givenUnknownUserAutenticated() {
        when(authentication.getName()).thenReturn(UNKNOWN_ID);
        when(usuarioRepository.findByIdCognito(UNKNOWN_ID)).thenReturn(Optional.empty());
    }

    private void whenRequireAdminAccess(Authentication auth) {
        adminAccessGuard.requireAdminAccess(auth);
    }

    private void thenExceptionMessageIs(String expectedMessage, Exception exception) {
        assertEquals(expectedMessage, exception.getMessage());
    }
}
