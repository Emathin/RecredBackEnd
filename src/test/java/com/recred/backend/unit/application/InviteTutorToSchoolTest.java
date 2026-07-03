package com.recred.backend.unit.application;

import com.recred.backend.application.InviteTutorToSchool;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.TutorInvitationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteTutorToSchoolTest {

    private static final String DIRECTOR_SUBJECT = "director-sub";
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
    @Mock
    private TutorInvitationEmailSender emailSender;
    @Captor
    private ArgumentCaptor<TutorInvitation> invitationCaptor;

    private InviteTutorToSchool useCase;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        useCase = new InviteTutorToSchool(
                userRepository,
                tutorRepository,
                tutorSchoolRepository,
                invitationRepository,
                emailSender,
                CLOCK,
                "https://app.test/");
        schoolId = UUID.randomUUID();
    }

    @Test
    void givenDirectorAndNewEmail_whenInvitingTutor_thenCreatesPendingInvitationAndSendsEmail() {
        givenDirectorWithSchool();
        givenTutorEmailIsNotRegistered();
        givenNoReusableInvitation();
        givenInvitationIsSaved();

        InviteTutorToSchool.TutorInvitationResult result = whenInvitingTutor();

        verify(invitationRepository).save(invitationCaptor.capture());
        verify(emailSender).sendInvitation(any(TutorInvitation.class),
                org.mockito.ArgumentMatchers.contains("/invitaciones/tutor?token="));
        TutorInvitation saved = invitationCaptor.getValue();
        assertEquals("parent@test.com", saved.getEmail());
        assertEquals(TutorInvitationStatus.PENDING, saved.getStatus());
        assertEquals(CLOCK.instant().atZone(ZoneOffset.UTC).toLocalDateTime().plusDays(7),
                saved.getExpiresAt());
        assertEquals("CREATED", result.status());
        assertNotNull(result.invitationLink());
    }

    private void givenDirectorWithSchool() {
        Usuario director = new Usuario();
        director.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        Colegio school = new Colegio();
        school.setId(schoolId);
        director.setColegio(school);
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.of(director));
    }

    private void givenTutorEmailIsNotRegistered() {
        when(userRepository.findByEmail("parent@test.com")).thenReturn(Optional.empty());
    }

    private void givenNoReusableInvitation() {
        when(invitationRepository.findReusableBySchoolIdAndEmail(schoolId, "parent@test.com"))
                .thenReturn(Optional.empty());
    }

    private void givenInvitationIsSaved() {
        when(invitationRepository.save(any(TutorInvitation.class))).thenAnswer(invocation -> {
            TutorInvitation invitation = invocation.getArgument(0);
            invitation.setId(UUID.randomUUID());
            return invitation;
        });
    }

    private InviteTutorToSchool.TutorInvitationResult whenInvitingTutor() {
        return useCase.execute(
                DIRECTOR_SUBJECT,
                new InviteTutorToSchool.TutorInvitationCommand(
                        " Parent@Test.com ",
                        " Ana ",
                        " Gomez ",
                        " 1111 "));
    }
}
