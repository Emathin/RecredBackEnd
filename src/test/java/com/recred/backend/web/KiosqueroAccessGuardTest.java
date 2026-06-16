package com.recred.backend.web;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KiosqueroAccessGuardTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final KiosqueroRepository kiosqueroRepository = mock(KiosqueroRepository.class);
    private final BuffetRepository buffetRepository = mock(BuffetRepository.class);
    private final KiosqueroAccessGuard guard = new KiosqueroAccessGuard(
            usuarioRepository,
            kiosqueroRepository,
            buffetRepository
    );

    @Test
    void requireBuffetAccess_WhenSellerOwnsBuffet_ShouldPass() {
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Usuario user = user(userId, RolUsuario.VENDEDOR, null);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);

        assertDoesNotThrow(() -> guard.requireBuffetAccess(auth(), buffetId));
    }

    @Test
    void requireBuffetAccess_WhenUserIsAdmin_ShouldPass() {
        UUID buffetId = UUID.randomUUID();
        Usuario user = user(UUID.randomUUID(), RolUsuario.ADMIN, null);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> guard.requireBuffetAccess(auth(), buffetId));
    }

    @Test
    void requireBuffetAccess_WhenSellerDoesNotOwnBuffet_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        Usuario user = user(userId, RolUsuario.VENDEDOR, null);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(UUID.randomUUID());

        assertThrows(AccessDeniedException.class,
                () -> guard.requireBuffetAccess(auth(), UUID.randomUUID()));
    }

    @Test
    void requireBuffetAccess_WhenSchoolDirectorOwnsSchool_ShouldPass() {
        UUID colegioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Usuario user = user(UUID.randomUUID(), RolUsuario.DIRECTIVO_COLEGIO, colegioId);
        Buffet buffet = buffet(colegioId);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));

        assertDoesNotThrow(() -> guard.requireBuffetAccess(auth(), buffetId));
    }

    @Test
    void requireBuffetAccess_WhenAuthenticationIsMissing_ShouldThrowException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> guard.requireBuffetAccess(null, UUID.randomUUID()));
    }

    @Test
    void requireBuffetAccess_WhenUserIsNotSynchronized_ShouldThrowException() {
        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.empty());

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> guard.requireBuffetAccess(auth(), UUID.randomUUID()));
    }

    @Test
    void requireBuffetAccess_WhenSchoolDirectorHasNoSchool_ShouldThrowException() {
        Usuario user = user(UUID.randomUUID(), RolUsuario.DIRECTIVO_COLEGIO, null);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class,
                () -> guard.requireBuffetAccess(auth(), UUID.randomUUID()));
    }

    @Test
    void requireBuffetAccess_WhenBuffetSchoolHasNoId_ShouldThrowException() {
        UUID colegioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Usuario user = user(UUID.randomUUID(), RolUsuario.DIRECTIVO_COLEGIO, colegioId);
        Buffet buffet = buffet(null);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));

        assertThrows(AccessDeniedException.class,
                () -> guard.requireBuffetAccess(auth(), buffetId));
    }

    private TestingAuthenticationToken auth() {
        return new TestingAuthenticationToken("sub", "");
    }

    private Usuario user(UUID userId, RolUsuario role, UUID colegioId) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        if (colegioId != null) {
            Colegio colegio = new Colegio();
            colegio.setId(colegioId);
            user.setColegio(colegio);
        }
        return user;
    }

    private Buffet buffet(UUID colegioId) {
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        return buffet;
    }
}
