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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    private void givenPendingInvitation() {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setEmail(EMAIL);
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now(CLOCK).plusDays(1));
        Colegio school = new Colegio();
        school.setId(schoolId);
        invitation.setSchool(school);
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
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
