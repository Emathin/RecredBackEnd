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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final String EMAIL_WITH_SPACES_AND_UPPERCASE = " Parent@Test.com ";
    private static final String DIFFERENT_EMAIL = "other@test.com";
    private static final String FIRST_NAME = "Ana";
    private static final String LAST_NAME = "Gomez";
    private static final String PHONE = "1111";
    private static final String DNI = "123";
    private static final String BLANK_VALUE = " ";
    private static final String EMPTY_VALUE = "";
    private static final String FALLBACK_FIRST_NAME = "Sin Nombre";
    private static final String FALLBACK_LAST_NAME = "Sin Apellido";

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 12, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC
    );

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
                CLOCK
        );

        schoolId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tutorId = UUID.randomUUID();
    }

    @Test
    void givenValidInvitationAndNewLocalUser_whenAccepting_thenCreatesTutorAndSchoolLink() {
        givenPendingInvitation();
        givenNoLocalUser();
        givenUserIsSavedWithGeneratedId();
        givenTutorDoesNotExistAndThenExists();
        givenNoTutorSchoolLink();
        givenInvitationIsSaved();

        TutorInvitation acceptedInvitation = whenAcceptingInvitation();

        thenNewParentUserWasSaved();
        thenTutorWasLinkedToSchool();
        thenInvitationWasAccepted(acceptedInvitation);
    }

    @Test
    void givenMissingInvitation_whenAccepting_thenFails() {
        givenInvitationDoesNotExist();

        thenAcceptingInvitationFailsBecauseInvitationDoesNotExist();
        thenUserTutorAndSchoolLinkWereNotTouched();
    }

    @Test
    void givenAcceptedInvitation_whenAccepting_thenFails() {
        givenAcceptedInvitation();

        thenAcceptingInvitationFailsBecauseInvitationIsNotPending();
        thenUserTutorAndSchoolLinkWereNotTouched();
    }

    @Test
    void givenExpiredInvitation_whenAccepting_thenMarksExpiredAndFails() {
        givenExpiredInvitation();
        givenInvitationIsSaved();

        thenAcceptingInvitationFailsBecauseInvitationIsExpired();
        thenInvitationWasMarkedAsExpired();
        thenUserTutorAndSchoolLinkWereNotTouched();
    }

    @Test
    void givenDifferentAuthenticatedEmail_whenAccepting_thenFails() {
        givenPendingInvitation();

        thenAcceptingInvitationWithDifferentEmailIsRejected();
        thenUserTutorAndSchoolLinkWereNotTouched();
    }

    @Test
    void givenExistingNonParentUserAndTutorSchool_whenAccepting_thenUpdatesAndActivatesLink() {
        givenPendingInvitation();
        givenExistingAdminUser();
        givenExistingTutorForUser();
        givenExistingRevokedTutorSchoolLink();
        givenInvitationIsSaved();

        TutorInvitation acceptedInvitation = whenAcceptingInvitation();

        thenExistingUserWasSavedAsParent();
        thenExistingTutorSchoolLinkWasActivated();
        thenInvitationWasAccepted(acceptedInvitation);
    }

    @Test
    void givenNewUserWithoutProfileData_whenAccepting_thenUsesFallbacks() {
        givenPendingInvitation();
        givenNoLocalUser();
        givenUserIsSavedWithGeneratedId();
        givenTutorDoesNotExistAndThenExists();
        givenNoTutorSchoolLink();
        givenInvitationIsSaved();

        whenAcceptingInvitationWithoutProfileData();

        thenNewParentUserWasSavedWithFallbackProfileData();
    }

    private void givenPendingInvitation() {
        givenInvitationExists(anInvitationWithStatus(TutorInvitationStatus.PENDING));
    }

    private void givenAcceptedInvitation() {
        givenInvitationExists(anInvitationWithStatus(TutorInvitationStatus.ACCEPTED));
    }

    private void givenExpiredInvitation() {
        givenInvitationExists(anExpiredInvitation());
    }

    private void givenInvitationDoesNotExist() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());
    }

    private void givenInvitationExists(TutorInvitation invitation) {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
    }

    private void givenNoLocalUser() {
        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenExistingAdminUser() {
        Usuario user = aUserWithRole(RolUsuario.ADMIN);

        when(userRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
    }

    private void givenUserIsSavedWithGeneratedId() {
        when(userRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
    }

    private void givenTutorDoesNotExistAndThenExists() {
        when(tutorRepository.findByUsuarioId(userId))
                .thenReturn(Optional.empty(), Optional.of(aTutor()));
    }

    private void givenExistingTutorForUser() {
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(aTutor()));
    }

    private void givenNoTutorSchoolLink() {
        when(tutorSchoolRepository.findByTutorIdAndSchoolId(tutorId, schoolId))
                .thenReturn(Optional.empty());
    }

    private void givenExistingRevokedTutorSchoolLink() {
        when(tutorSchoolRepository.findByTutorIdAndSchoolId(tutorId, schoolId))
                .thenReturn(Optional.of(aTutorSchoolWithStatus(TutorSchoolStatus.REVOKED)));
    }

    private void givenInvitationIsSaved() {
        when(invitationRepository.save(any(TutorInvitation.class))).thenAnswer(invocation ->
                invocation.getArgument(0)
        );
    }

    private TutorInvitation whenAcceptingInvitation() {
        return useCase.execute(aValidAcceptRequest());
    }

    private void whenAcceptingInvitationWithoutProfileData() {
        useCase.execute(anAcceptRequestWithoutProfileData());
    }

    private void thenNewParentUserWasSaved() {
        Usuario savedUser = firstSavedUser();

        assertEquals(RolUsuario.PADRE, savedUser.getRol());
        assertEquals(EMAIL, savedUser.getEmail());
    }

    private void thenNewParentUserWasSavedWithFallbackProfileData() {
        Usuario savedUser = firstSavedUser();

        assertEquals(FALLBACK_FIRST_NAME, savedUser.getNombre());
        assertEquals(FALLBACK_LAST_NAME, savedUser.getApellido());
        assertNull(savedUser.getTelefono());
        assertNull(savedUser.getDni());
    }

    private void thenExistingUserWasSavedAsParent() {
        Usuario savedUser = onlySavedUser();

        assertEquals(RolUsuario.PADRE, savedUser.getRol());
        assertEquals(NOW, savedUser.getActualizadoEn());
    }

    private void thenTutorWasLinkedToSchool() {
        TutorSchool tutorSchool = savedTutorSchool();

        assertEquals(tutorId, tutorSchool.getTutorId());
        assertEquals(schoolId, tutorSchool.getSchool().getId());
        assertEquals(TutorSchoolStatus.ACTIVE, tutorSchool.getStatus());
    }

    private void thenExistingTutorSchoolLinkWasActivated() {
        TutorSchool tutorSchool = savedTutorSchool();

        assertEquals(TutorSchoolStatus.ACTIVE, tutorSchool.getStatus());
    }

    private void thenInvitationWasAccepted(TutorInvitation invitation) {
        assertEquals(TutorInvitationStatus.ACCEPTED, invitation.getStatus());
        assertEquals(NOW, invitation.getUsedAt());
    }

    private void thenInvitationWasMarkedAsExpired() {
        verify(invitationRepository).save(invitationCaptor.capture());

        assertEquals(TutorInvitationStatus.EXPIRED, invitationCaptor.getValue().getStatus());
    }

    private void thenAcceptingInvitationFailsBecauseInvitationDoesNotExist() {
        thenAcceptingInvitationFailsWith(
                IllegalArgumentException.class,
                "No se encontro la invitacion"
        );
    }

    private void thenAcceptingInvitationFailsBecauseInvitationIsNotPending() {
        thenAcceptingInvitationFailsWith(
                IllegalArgumentException.class,
                "La invitacion no esta pendiente"
        );
    }

    private void thenAcceptingInvitationFailsBecauseInvitationIsExpired() {
        thenAcceptingInvitationFailsWith(
                IllegalArgumentException.class,
                "La invitacion esta vencida"
        );
    }

    private void thenAcceptingInvitationWithDifferentEmailIsRejected() {
        assertThrows(AccessDeniedException.class, acceptingInvitationWithDifferentEmail());
    }

    private <T extends Throwable> void thenAcceptingInvitationFailsWith(
            Class<T> expectedException,
            String expectedMessage
    ) {
        T exception = assertThrows(expectedException, acceptingInvitation());

        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenUserTutorAndSchoolLinkWereNotTouched() {
        verifyNoInteractions(userRepository, tutorRepository, tutorSchoolRepository);
    }

    private Usuario firstSavedUser() {
        verify(userRepository, times(2)).save(userCaptor.capture());

        return userCaptor.getAllValues().get(0);
    }

    private Usuario onlySavedUser() {
        verify(userRepository).save(userCaptor.capture());

        return userCaptor.getValue();
    }

    private TutorSchool savedTutorSchool() {
        verify(tutorSchoolRepository).save(tutorSchoolCaptor.capture());

        return tutorSchoolCaptor.getValue();
    }

    private Executable acceptingInvitation() {
        return () -> whenAcceptingInvitation();
    }

    private Executable acceptingInvitationWithDifferentEmail() {
        return () -> useCase.execute(anAcceptRequestWithDifferentEmail());
    }

    private AcceptTutorInvitationRequest aValidAcceptRequest() {
        return anAcceptRequest(
                EMAIL_WITH_SPACES_AND_UPPERCASE,
                FIRST_NAME,
                LAST_NAME,
                PHONE,
                DNI
        );
    }

    private AcceptTutorInvitationRequest anAcceptRequestWithDifferentEmail() {
        return anAcceptRequest(
                DIFFERENT_EMAIL,
                FIRST_NAME,
                LAST_NAME,
                null,
                null
        );
    }

    private AcceptTutorInvitationRequest anAcceptRequestWithoutProfileData() {
        return anAcceptRequest(
                EMAIL,
                BLANK_VALUE,
                null,
                BLANK_VALUE,
                EMPTY_VALUE
        );
    }

    private AcceptTutorInvitationRequest anAcceptRequest(
            String email,
            String firstName,
            String lastName,
            String phone,
            String dni
    ) {
        return new AcceptTutorInvitationRequest(
                TOKEN,
                COGNITO_SUBJECT,
                email,
                firstName,
                lastName,
                phone,
                dni
        );
    }

    private TutorInvitation anInvitationWithStatus(TutorInvitationStatus status) {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setEmail(EMAIL);
        invitation.setStatus(status);
        invitation.setExpiresAt(NOW.plusDays(1));
        invitation.setSchool(aSchool());
        return invitation;
    }

    private TutorInvitation anExpiredInvitation() {
        TutorInvitation invitation = anInvitationWithStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(NOW.minusMinutes(1));
        return invitation;
    }

    private Colegio aSchool() {
        Colegio school = new Colegio();
        school.setId(schoolId);
        return school;
    }

    private Usuario aUserWithRole(RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        return user;
    }

    private Tutor aTutor() {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        return tutor;
    }

    private TutorSchool aTutorSchoolWithStatus(TutorSchoolStatus status) {
        TutorSchool tutorSchool = new TutorSchool();
        tutorSchool.setStatus(status);
        return tutorSchool;
    }
}