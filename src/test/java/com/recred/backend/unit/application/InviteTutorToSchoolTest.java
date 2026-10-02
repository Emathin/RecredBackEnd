package com.recred.backend.unit.application;

import com.recred.backend.application.InviteTutorToSchool;
import com.recred.backend.application.InviteTutorToSchoolRequest;
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
    private static final String TUTOR_EMAIL = "parent@test.com";
    private static final UUID SCHOOL_ID = UUID.randomUUID();
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

    private InviteTutorToSchool useCase;

    @Captor
    private ArgumentCaptor<TutorInvitation> invitationCaptor;

    @BeforeEach
    void setUp() {
        useCase = new InviteTutorToSchool(
                userRepository, tutorRepository, tutorSchoolRepository,
                invitationRepository, emailSender, CLOCK, "https://app.test/");
    }

    @Test
    void shouldCreatePendingInvitationAndSendEmail_whenInvitingNewTutor() {
        givenDirectorWithSchool();
        givenTutorEmailIsNotRegistered();
        givenNoReusableInvitation();
        givenInvitationIsSaved();

        InviteTutorToSchool.TutorInvitationResult result = whenInvitingTutor();

        thenInvitationWasSavedAndEmailSent(result);
    }

    @Test
    void shouldReturnAlreadyAssociated_whenTutorIsAlreadyInSchool() {
        givenDirectorWithSchool();
        givenTutorIsAlreadyAssociated();

        InviteTutorToSchool.TutorInvitationResult result = whenInvitingTutor();

        thenResultIsAlreadyAssociated(result);
    }

    @Test
    void shouldReturnResentEmailError_whenReusingInvitationAndEmailFails() {
        givenDirectorWithSchool();
        givenTutorEmailIsNotRegistered();
        givenReusableInvitation();
        givenInvitationIsSaved();
        givenEmailSenderThrowsError();

        InviteTutorToSchool.TutorInvitationResult result = whenInvitingTutor();

        thenInvitationWasUpdatedAndResultIsEmailError(result);
    }

    @Test
    void shouldThrowException_whenDirectorIsMissing() {
        givenUserNotFound();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                this::whenInvitingTutor);

        thenExceptionMessageIs(exception, "No se encontro el usuario autenticado");
        thenNoInteractionsWithInvitationOrEmail();
    }

    @Test
    void shouldThrowException_whenUserIsNotDirector() {
        givenNonDirectorUser();

        assertThrows(AccessDeniedException.class, this::whenInvitingTutor);

        thenNoInteractionsWithInvitationOrEmail();
    }

    @Test
    void shouldThrowException_whenDirectorHasNoSchool() {
        givenDirectorWithoutSchool();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                this::whenInvitingTutor);

        thenExceptionMessageIs(exception, "El usuario autenticado no tiene un colegio asignado");
        thenNoInteractionsWithInvitationOrEmail();
    }

    private void givenDirectorWithSchool() {
        Usuario director = new Usuario();
        director.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        Colegio school = new Colegio();
        school.setId(SCHOOL_ID);
        director.setColegio(school);
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.of(director));
    }

    private void givenTutorEmailIsNotRegistered() {
        when(userRepository.findByEmail(TUTOR_EMAIL)).thenReturn(Optional.empty());
    }

    private void givenNoReusableInvitation() {
        when(invitationRepository.findReusableBySchoolIdAndEmail(SCHOOL_ID, TUTOR_EMAIL))
                .thenReturn(Optional.empty());
    }

    private void givenReusableInvitation() {
        TutorInvitation reusable = new TutorInvitation();
        reusable.setId(UUID.randomUUID());
        reusable.setUsedAt(LocalDateTime.now(CLOCK).minusDays(1));
        reusable.setStatus(TutorInvitationStatus.EXPIRED);
        when(invitationRepository.findReusableBySchoolIdAndEmail(SCHOOL_ID, TUTOR_EMAIL))
                .thenReturn(Optional.of(reusable));
    }

    private void givenInvitationIsSaved() {
        when(invitationRepository.save(any(TutorInvitation.class))).thenAnswer(invocation -> {
            TutorInvitation invitation = invocation.getArgument(0);
            if (invitation.getId() == null) {
                invitation.setId(UUID.randomUUID());
            }
            return invitation;
        });
    }

    private void givenTutorIsAlreadyAssociated() {
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Usuario existingUser = new Usuario();
        existingUser.setId(userId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        
        when(userRepository.findByEmail(TUTOR_EMAIL)).thenReturn(Optional.of(existingUser));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
        when(tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                tutorId, SCHOOL_ID, TutorSchoolStatus.ACTIVE)).thenReturn(true);
    }

    private void givenEmailSenderThrowsError() {
        doThrow(new RuntimeException("smtp down")).when(emailSender)
                .sendInvitation(any(TutorInvitation.class), any(String.class));
    }

    private void givenUserNotFound() {
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenNonDirectorUser() {
        Usuario user = new Usuario();
        user.setRol(RolUsuario.PADRE);
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.of(user));
    }

    private void givenDirectorWithoutSchool() {
        Usuario director = new Usuario();
        director.setRol(RolUsuario.DIRECTIVO_COLEGIO);
        when(userRepository.findByIdCognito(DIRECTOR_SUBJECT)).thenReturn(Optional.of(director));
    }

    private InviteTutorToSchool.TutorInvitationResult whenInvitingTutor() {
        InviteTutorToSchoolRequest request = new InviteTutorToSchoolRequest(
                DIRECTOR_SUBJECT,
                " Parent@Test.com ",
                " Ana ",
                " Gomez ",
                " 1111 ");
        return useCase.execute(request);
    }



    private void thenInvitationWasSavedAndEmailSent(InviteTutorToSchool.TutorInvitationResult result) {
        verify(invitationRepository).save(invitationCaptor.capture());
        verify(emailSender).sendInvitation(any(TutorInvitation.class),
                org.mockito.ArgumentMatchers.contains("/invitaciones/tutor?token="));
        
        TutorInvitation saved = invitationCaptor.getValue();
        assertEquals(TUTOR_EMAIL, saved.getEmail());
        assertEquals(TutorInvitationStatus.PENDING, saved.getStatus());
        assertEquals(CLOCK.instant().atZone(ZoneOffset.UTC).toLocalDateTime().plusDays(7),
                saved.getExpiresAt());
        
        assertEquals("CREATED", result.status());
        assertNotNull(result.invitationLink());
    }

    private void thenResultIsAlreadyAssociated(InviteTutorToSchool.TutorInvitationResult result) {
        assertEquals("ALREADY_ASSOCIATED", result.status());
        assertNull(result.invitation());
        assertNull(result.invitationLink());
        thenNoInteractionsWithInvitationOrEmail();
    }

    private void thenInvitationWasUpdatedAndResultIsEmailError(
            InviteTutorToSchool.TutorInvitationResult result) {
        verify(invitationRepository).save(invitationCaptor.capture());
        TutorInvitation saved = invitationCaptor.getValue();
        
        assertEquals("RESENT_EMAIL_ERROR", result.status());
        assertNotNull(result.invitationLink());
        assertEquals(TutorInvitationStatus.PENDING, saved.getStatus());
        assertEquals("Ana", saved.getFirstName());
        assertEquals("Gomez", saved.getLastName());
        assertEquals("1111", saved.getPhone());
        assertNull(saved.getUsedAt());
    }

    private void thenExceptionMessageIs(IllegalArgumentException exception, String expected) {
        assertEquals(expected, exception.getMessage());
    }

    private void thenNoInteractionsWithInvitationOrEmail() {
        verifyNoInteractions(invitationRepository, emailSender);
    }
}
