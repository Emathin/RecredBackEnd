package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import org.junit.jupiter.api.BeforeEach;
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

    private UsuarioRepository usuarioRepository;
    private KiosqueroRepository kiosqueroRepository;
    private BuffetRepository buffetRepository;
    private KiosqueroAccessGuard guard;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID BUFFET_ID = UUID.randomUUID();
    private final UUID COLEGIO_ID = UUID.randomUUID();
    private final String SUB = "sub";

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        kiosqueroRepository = mock(KiosqueroRepository.class);
        buffetRepository = mock(BuffetRepository.class);
        guard = new KiosqueroAccessGuard(usuarioRepository, kiosqueroRepository, buffetRepository);
    }

    @Test
    void requireBuffetAccessShouldPassWhenSellerOwnsBuffet() {
        givenUserIsSeller();
        givenSellerOwnsBuffet();
        thenAccessIsGranted();
    }

    @Test
    void requireBuffetAccessShouldPassWhenUserIsAdmin() {
        givenUserIsAdmin();
        thenAccessIsGranted();
    }

    @Test
    void requireBuffetAccessShouldThrowExceptionWhenSellerDoesNotOwnBuffet() {
        givenUserIsSeller();
        givenSellerDoesNotOwnBuffet();
        thenAccessIsDenied();
    }

    @Test
    void requireBuffetAccessShouldPassWhenSchoolDirectorOwnsSchool() {
        givenUserIsSchoolDirector();
        givenBuffetBelongsToSchool();
        thenAccessIsGranted();
    }

    @Test
    void requireBuffetAccessShouldThrowExceptionWhenAuthenticationIsMissing() {
        thenAuthenticationIsRequired(null);
    }

    @Test
    void requireBuffetAccessShouldThrowExceptionWhenUserIsNotSynchronized() {
        givenUserDoesNotExist();
        thenAuthenticationIsRequired(auth());
    }

    @Test
    void requireBuffetAccessShouldThrowExceptionWhenSchoolDirectorHasNoSchool() {
        givenUserIsSchoolDirectorWithoutSchool();
        thenAccessIsDenied();
    }

    @Test
    void requireBuffetAccessShouldThrowExceptionWhenBuffetSchoolHasNoId() {
        givenUserIsSchoolDirector();
        givenBuffetHasNoSchool();
        thenAccessIsDenied();
    }

    private void givenUserIsSeller() {
        Usuario user = user(USER_ID, RolUsuario.VENDEDOR, null);
        when(usuarioRepository.findByIdCognito(SUB)).thenReturn(Optional.of(user));
    }

    private void givenUserIsAdmin() {
        Usuario user = user(USER_ID, RolUsuario.ADMIN, null);
        when(usuarioRepository.findByIdCognito(SUB)).thenReturn(Optional.of(user));
    }

    private void givenUserIsSchoolDirector() {
        Usuario user = user(USER_ID, RolUsuario.DIRECTIVO_COLEGIO, COLEGIO_ID);
        when(usuarioRepository.findByIdCognito(SUB)).thenReturn(Optional.of(user));
    }

    private void givenUserIsSchoolDirectorWithoutSchool() {
        Usuario user = user(USER_ID, RolUsuario.DIRECTIVO_COLEGIO, null);
        when(usuarioRepository.findByIdCognito(SUB)).thenReturn(Optional.of(user));
    }

    private void givenUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(SUB)).thenReturn(Optional.empty());
    }

    private void givenSellerOwnsBuffet() {
        when(kiosqueroRepository.findBuffetByUsuarioId(USER_ID)).thenReturn(BUFFET_ID);
    }

    private void givenSellerDoesNotOwnBuffet() {
        when(kiosqueroRepository.findBuffetByUsuarioId(USER_ID)).thenReturn(UUID.randomUUID());
    }

    private void givenBuffetBelongsToSchool() {
        Buffet buffet = buffet(COLEGIO_ID);
        when(buffetRepository.findById(BUFFET_ID)).thenReturn(Optional.of(buffet));
    }

    private void givenBuffetHasNoSchool() {
        Buffet buffet = buffet(null);
        when(buffetRepository.findById(BUFFET_ID)).thenReturn(Optional.of(buffet));
    }

    private void thenAccessIsGranted() {
        assertDoesNotThrow(() -> guard.requireBuffetAccess(auth(), BUFFET_ID));
    }

    private void thenAccessIsDenied() {
        assertThrows(AccessDeniedException.class, () -> guard.requireBuffetAccess(auth(), BUFFET_ID));
    }

    private void thenAuthenticationIsRequired(TestingAuthenticationToken auth) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> guard.requireBuffetAccess(auth, BUFFET_ID));
    }

    private TestingAuthenticationToken auth() {
        return new TestingAuthenticationToken(SUB, "");
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
        Buffet buffet = new Buffet();
        if (colegioId != null) {
            Colegio colegio = new Colegio();
            colegio.setId(colegioId);
            buffet.setColegio(colegio);
        }
        return buffet;
    }
}
