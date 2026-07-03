package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchoolAccessGuardTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final KiosqueroRepository kiosqueroRepository = mock(KiosqueroRepository.class);
    private final BuffetRepository buffetRepository = mock(BuffetRepository.class);
    private final FranjaHorariaRepository franjaHorariaRepository =
            mock(FranjaHorariaRepository.class);
    private final SchoolAccessGuard guard = new SchoolAccessGuard(
            usuarioRepository,
            kiosqueroRepository,
            buffetRepository,
            franjaHorariaRepository
    );

    @Test
    void requireSchoolAccess_WhenDirectorBelongsToSchool_ShouldPass() {
        UUID colegioId = UUID.randomUUID();
        Usuario user = user(UUID.randomUUID(), RolUsuario.DIRECTIVO_COLEGIO, colegioId);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> guard.requireSchoolAccess(auth(), colegioId));
    }

    @Test
    void requireSchoolAccess_WhenSellerOwnsBuffetInSchool_ShouldPass() {
        UUID userId = UUID.randomUUID();
        UUID colegioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Usuario user = user(userId, RolUsuario.VENDEDOR, null);

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet(colegioId)));

        assertDoesNotThrow(() -> guard.requireSchoolAccess(auth(), colegioId));
    }

    @Test
    void requireSchoolAccess_WhenSchoolDoesNotMatch_ShouldThrowException() {
        UUID colegioId = UUID.randomUUID();
        Usuario user = user(UUID.randomUUID(), RolUsuario.DIRECTIVO_COLEGIO, UUID.randomUUID());

        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class,
                () -> guard.requireSchoolAccess(auth(), colegioId));
    }

    @Test
    void requireTimeSlotAccess_ShouldResolveSchoolFromSlot() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Usuario user = user(UUID.randomUUID(), RolUsuario.ADMIN, null);
        FranjaHoraria slot = new FranjaHoraria();
        slot.setColegioId(colegioId);

        when(franjaHorariaRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> guard.requireTimeSlotAccess(auth(), slotId));
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
