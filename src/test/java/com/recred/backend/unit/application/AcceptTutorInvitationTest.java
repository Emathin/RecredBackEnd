package com.recred.backend.unit.application;

import com.recred.backend.application.AcceptTutorInvitation;
import com.recred.backend.application.AcceptTutorInvitationRequest;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.TutorSchool;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptTutorInvitationTest {

    private static final String TOKEN = "plain-token";
    private static final String COGNITO_SUBJECT = "parent-sub";
    private static final String EMAIL = "parent@test.com";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorSchoolRepository tutorSchoolRepository;
    @Mock
    private TutorInvitationRepository invitationRepository;
    @Captor
    private ArgumentCaptor<Usuario> userCaptor;
    @Captor
    private ArgumentCaptor<TutorSchool> tutorSchoolCaptor;
    @Captor
    private ArgumentCaptor<TutorInvitation> invitationCaptor;

    private AcceptTutorInvitation useCase;
    private UUID schoolId;
    private UUID userId;
    private UUID tutorId;

    @BeforeEach
    void setUp() {
        useCase = new AcceptTutorInvitation(
                userRepository,
                tutorRepository,
                tutorSchoolRepository,
                invitationRepository,
                CLOCK);
        schoolId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tutorId = UUID.randomUUID();
    }

    @Test
    void givenValidInvitationAndNewLocalUser_whenAccepting_thenCreatesTutorAndSchoolLink() {
        givenPendingInvitation();
        givenNoLocalUser();
        givenUserIsSaved();
        givenTutorDoesNotExistThenExists();
        givenNoTutorSchoolLink();
        givenInvitationIsSaved();

        TutorInvitation result = whenAcceptingInvitation();

        verify(userRepository, times(2)).save(userCaptor.capture());
        verify(tutorSchoolRepository).save(tutorSchoolCaptor.capture());
        Usuario savedUser = userCaptor.getAllValues().get(0);
        TutorSchool tutorSchool = tutorSchoolCaptor.getValue();
        assertEquals(RolUsuario.PADRE, savedUser.getRol());
        assertEquals(EMAIL, savedUser.getEmail());
        assertEquals(tutorId, tutorSchool.getTutorId());
        assertEquals(schoolId, tutorSchool.getSchool().getId());
        assertEquals(TutorSchoolStatus.ACTIVE, tutorSchool.getStatus());
        assertEquals(TutorInvitationStatus.ACCEPTED, result.getStatus());
        assertTrue(result.getUsedAt().isEqual(LocalDateTime.now(CLOCK)));
    }

    @Test
    void givenMissingInvitation_whenAccepting_thenFails() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> whenAcceptingInvitation()
        );

        assertEquals("No se encontro la invitacion", exception.getMessage());
        verifyNoInteractions(userRepository, tutorRepository, tutorSchoolRepository);
    }

    @Test
    void givenAcceptedInvitation_whenAccepting_thenFails() {
        TutorInvitation invitation = invitation(TutorInvitationStatus.ACCEPTED);
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> whenAcceptingInvitation()
        );

        assertEquals("La invitacion no esta pendiente", exception.getMessage());
        verifyNoInteractions(userRepository, tutorRepository, tutorSchoolRepository);
    }

    @Test
    void givenExpiredInvitation_whenAccepting_thenMarksExpiredAndFails() {
        TutorInvitation invitation = invitation(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now(CLOCK).minusMinutes(1));
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
        givenInvitationIsSaved();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> whenAcceptingInvitation()
        );

        assertEquals("La invitacion esta vencida", exception.getMessage());
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals(TutorInvitationStatus.EXPIRED, invitationCaptor.getValue().getStatus());
        verifyNoInteractions(userRepository, tutorRepository, tutorSchoolRepository);
    }

    @Test
    void givenDifferentAuthenticatedEmail_whenAccepting_thenFails() {
        givenPendingInvitation();

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(new AcceptTutorInvitationRequest(
                        TOKEN,
                        COGNITO_SUBJECT,
                        "other@test.com",
                        "Ana",
                        "Gomez",
                        null,
                        null
                )));
        verifyNoInteractions(userRepository, tutorRepository, tutorSchoolRepository);
    }

    @Test
    void givenExistingNonParentUserAndTutorSchool_whenAccepting_thenUpdatesAndActivatesLink() {
        TutorInvitation invitation = invitation(TutorInvitationStatus.PENDING);
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(RolUsuario.ADMIN);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        TutorSchool tutorSchool = new TutorSchool();
        tutorSchool.setStatus(TutorSchoolStatus.REVOKED);

        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
        when(tutorSchoolRepository.findByTutorIdAndSchoolId(tutorId, schoolId))
                .thenReturn(Optional.of(tutorSchool));
        givenInvitationIsSaved();

        TutorInvitation result = whenAcceptingInvitation();

        verify(userRepository).save(userCaptor.capture());
        verify(tutorSchoolRepository).save(tutorSchoolCaptor.capture());
        assertEquals(RolUsuario.PADRE, userCaptor.getValue().getRol());
        assertEquals(LocalDateTime.now(CLOCK), userCaptor.getValue().getActualizadoEn());
        assertEquals(TutorSchoolStatus.ACTIVE, tutorSchoolCaptor.getValue().getStatus());
        assertEquals(TutorInvitationStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void givenNewUserWithoutProfileData_whenAccepting_thenUsesFallbacks() {
        givenPendingInvitation();
        givenNoLocalUser();
        givenUserIsSaved();
        givenTutorDoesNotExistThenExists();
        givenNoTutorSchoolLink();
        givenInvitationIsSaved();

        useCase.execute(new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                "parent@test.com",
                " ",
                null,
                " ",
                ""
        ));

        verify(userRepository, times(2)).save(userCaptor.capture());
        Usuario savedUser = userCaptor.getAllValues().get(0);
        assertEquals("Sin Nombre", savedUser.getNombre());
        assertEquals("Sin Apellido", savedUser.getApellido());
        assertNull(savedUser.getTelefono());
        assertNull(savedUser.getDni());
    }

    private void givenPendingInvitation() {
        TutorInvitation invitation = invitation(TutorInvitationStatus.PENDING);
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }

    private TutorInvitation invitation(TutorInvitationStatus status) {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setEmail(EMAIL);
        invitation.setStatus(status);
        invitation.setExpiresAt(LocalDateTime.now(CLOCK).plusDays(1));
        Colegio school = new Colegio();
        school.setId(schoolId);
        invitation.setSchool(school);
        return invitation;
    }

    private void givenNoLocalUser() {
        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenUserIsSaved() {
        when(userRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
    }

    private void givenTutorDoesNotExistThenExists() {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        when(tutorRepository.findByUsuarioId(userId))
                .thenReturn(Optional.empty(), Optional.of(tutor));
    }

    private void givenNoTutorSchoolLink() {
        when(tutorSchoolRepository.findByTutorIdAndSchoolId(tutorId, schoolId))
                .thenReturn(Optional.empty());
    }

    private void givenInvitationIsSaved() {
        when(invitationRepository.save(any(TutorInvitation.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private TutorInvitation whenAcceptingInvitation() {
        return useCase.execute(new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                " Parent@Test.com ",
                "Ana",
                "Gomez",
                "1111",
                "123"));
    }
}
