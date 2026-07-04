package com.recred.backend.unit.application;

import com.recred.backend.application.InviteTutorToSchool;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.TutorInvitationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void givenTutorAlreadyAssociated_whenInvitingTutor_thenReturnsAlreadyAssociated() {
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Usuario existingUser = new Usuario();
        existingUser.setId(userId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        givenDirectorWithSchool();
        when(userRepository.findByEmail("parent@test.com")).thenReturn(Optional.of(existingUser));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
        when(tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                tutorId,
                schoolId,
                TutorSchoolStatus.ACTIVE
        )).thenReturn(true);

        InviteTutorToSchool.TutorInvitationResult result = whenInvitingTutor();

        assertEquals("ALREADY_ASSOCIATED", result.status());
        assertNull(result.invitation());
        assertNull(result.invitationLink());
        verifyNoInteractions(invitationRepository, emailSender);
    }

    @Test
    void givenReusableInvitationAndEmailFailure_whenInvitingTutor_thenReturnsResentEmailError() {
        TutorInvitation reusable = new TutorInvitation();
        reusable.setId(UUID.randomUUID());
        reusable.setUsedAt(LocalDateTime.now(CLOCK).minusDays(1));
        reusable.setStatus(TutorInvitationStatus.EXPIRED);
        givenDirectorWithSchool();
        givenTutorEmailIsNotRegistered();
        when(invitationRepository.findReusableBySchoolIdAndEmail(schoolId, "parent@test.com"))
                .thenReturn(Optional.of(reusable));
        givenInvitationIsSaved();
        doThrow(new RuntimeException("smtp down")).when(emailSender)
                .sendInvitation(any(TutorInvitation.class), any(String.class));
        InviteTutorToSchool.TutorInvitationCommand command =
                new InviteTutorToSchool.TutorInvitationCommand(
                        "parent@test.com",
                        " ",
                        null,
                        "\t"
                );

        InviteTutorToSchool.TutorInvitationResult result = useCase.execute(DIRECTOR_SUBJECT, command);

        verify(invitationRepository).save(invitationCaptor.capture());
        TutorInvitation saved = invitationCaptor.getValue();
        assertEquals("RESENT_EMAIL_ERROR", result.status());
        assertNotNull(result.invitationLink());
        assertEquals(TutorInvitationStatus.PENDING, saved.getStatus());
        assertNull(saved.getFirstName());
        assertNull(saved.getLastName());
        assertNull(saved.getPhone());
        assertNull(saved.getUsedAt());
    }

    @Test
    void givenMissingDirector_whenInvitingTutor_thenFails() {
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.empty());

        Executable execution = this::whenInvitingTutor;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("No se encontro el usuario autenticado", exception.getMessage());
        verifyNoInteractions(invitationRepository, emailSender);
    }

    @Test
    void givenNonDirectorUser_whenInvitingTutor_thenFails() {
        Usuario user = new Usuario();
        user.setRol(RolUsuario.PADRE);
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.of(user));

        Executable execution = this::whenInvitingTutor;

        assertThrows(AccessDeniedException.class, execution);
        verifyNoInteractions(invitationRepository, emailSender);
    }

    @Test
    void givenDirectorWithoutSchool_whenInvitingTutor_thenFails() {
        Usuario director = new Usuario();
        director.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.of(director));

        Executable execution = this::whenInvitingTutor;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("El usuario autenticado no tiene un colegio asignado",
                exception.getMessage());
        verifyNoInteractions(invitationRepository, emailSender);
    }

    @Test
    void givenInvalidEmail_whenInvitingTutor_thenFails() {
        givenDirectorWithSchool();
        InviteTutorToSchool.TutorInvitationCommand command =
                new InviteTutorToSchool.TutorInvitationCommand(
                        "no-es-email",
                        "Ana",
                        "Gomez",
                        null
                );

        Executable execution = () -> useCase.execute(DIRECTOR_SUBJECT, command);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("El email no es valido", exception.getMessage());
        verifyNoInteractions(invitationRepository, emailSender);
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
