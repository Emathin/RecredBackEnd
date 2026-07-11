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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAccessGuardTest {

    private static final String COGNITO_SUBJECT = "sub";
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private KiosqueroRepository kiosqueroRepository;
    @Mock private BuffetRepository buffetRepository;
    @Mock private FranjaHorariaRepository franjaHorariaRepository;

    private SchoolAccessGuard guard;
    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        guard = new SchoolAccessGuard(
                usuarioRepository,
                kiosqueroRepository,
                buffetRepository,
                franjaHorariaRepository
        );
        authentication = new TestingAuthenticationToken(COGNITO_SUBJECT, "");
    }

    @Test
    void requireSchoolAccessWhenDirectorBelongsToSchoolShouldPass() {
        givenAuthenticatedUser(RolUsuario.DIRECTIVO_COLEGIO, SCHOOL_ID);

        Executable execution = this::whenRequiringSchoolAccess;

        thenAccessIsGranted(execution);
    }

    @Test
    void requireSchoolAccessWhenSellerOwnsBuffetInSchoolShouldPass() {
        givenAuthenticatedUser(RolUsuario.VENDEDOR, null);
        givenSellerOwnsBuffetInSchool();

        Executable execution = this::whenRequiringSchoolAccess;

        thenAccessIsGranted(execution);
    }

    @Test
    void requireSchoolManagementAccessWhenSellerOwnsBuffetInSchoolShouldThrowException() {
        givenAuthenticatedUser(RolUsuario.VENDEDOR, null);

        Executable execution = this::whenRequiringSchoolManagementAccess;

        thenAccessIsDenied(execution);
    }

    @Test
    void requireSchoolAccessWhenSchoolDoesNotMatchShouldThrowException() {
        givenAuthenticatedUser(RolUsuario.DIRECTIVO_COLEGIO, UUID.randomUUID());

        Executable execution = this::whenRequiringSchoolAccess;

        thenAccessIsDenied(execution);
    }

    @Test
    void requireTimeSlotAccessShouldResolveSchoolFromSlot() {
        givenAuthenticatedUser(RolUsuario.ADMIN, null);
        givenTimeSlotBelongsToSchool();

        Executable execution = this::whenRequiringTimeSlotAccess;

        thenAccessIsGranted(execution);
    }

    private void givenAuthenticatedUser(RolUsuario role, UUID schoolId) {
        Usuario user = new Usuario();
        user.setId(USER_ID);
        user.setRol(role);
        if (schoolId != null) {
            Colegio school = new Colegio();
            school.setId(schoolId);
            user.setColegio(school);
        }
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
    }

    private void givenSellerOwnsBuffetInSchool() {
        Colegio school = new Colegio();
        school.setId(SCHOOL_ID);
        Buffet buffet = new Buffet();
        buffet.setColegio(school);

        when(kiosqueroRepository.findBuffetByUsuarioId(USER_ID)).thenReturn(BUFFET_ID);
        when(buffetRepository.findById(BUFFET_ID)).thenReturn(Optional.of(buffet));
    }

    private void givenTimeSlotBelongsToSchool() {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setColegioId(SCHOOL_ID);
        when(franjaHorariaRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    }

    private void whenRequiringSchoolAccess() {
        guard.requireSchoolAccess(authentication, SCHOOL_ID);
    }

    private void whenRequiringSchoolManagementAccess() {
        guard.requireSchoolManagementAccess(authentication, SCHOOL_ID);
    }

    private void whenRequiringTimeSlotAccess() {
        guard.requireTimeSlotAccess(authentication, SLOT_ID);
    }

    private void thenAccessIsGranted(Executable execution) {
        assertDoesNotThrow(execution);
    }

    private void thenAccessIsDenied(Executable execution) {
        assertThrows(AccessDeniedException.class, execution);
    }
}
