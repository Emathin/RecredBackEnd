package com.recred.backend.unit.application;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.domain.exceptions.UsernameExistsException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoDirectorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveSchoolRegistrationTest {

    private static final String SCHOOL_NAME = "Test School";
    private static final String SCHOOL_EMAIL = "test@school.com";
    private static final String SCHOOL_PHONE = "123456789";
    private static final String SCHOOL_CUE = "12345";
    private static final String DIRECTOR_FIRST_NAME = "John";
    private static final String DIRECTOR_LAST_NAME = "Doe";
    private static final String DIRECTOR_EMAIL = "john@doe.com";
    private static final String DIRECTOR_PHONE = "987654321";
    private static final String DIRECTOR_DNI = "12345678";
    private static final String DIRECTOR_USERNAME = "johndoe123";
    private static final String DIRECTOR_COGNITO_SUB = "sub-123";
    private static final String RETRY_DIRECTOR_COGNITO_SUB = "sub-retry";

    @Mock
    private SchoolRegistrationRequestRepository registrationRepository;
    @Mock
    private ColegioRepository colegioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CognitoDirectorIdentityService cognitoService;

    @Captor
    private ArgumentCaptor<Colegio> schoolCaptor;
    @Captor
    private ArgumentCaptor<Usuario> directorCaptor;

    private ApproveSchoolRegistration approveSchoolRegistration;
    private ApproveSchoolRegistrationRequest approvalRequest;
    private SchoolRegistrationRequest registration;
    private Colegio savedSchool;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        approveSchoolRegistration = new ApproveSchoolRegistration(
                registrationRepository,
                colegioRepository,
                usuarioRepository,
                cognitoService
        );
    }

    @Test
    void givenPendingRegistration_whenApprovingRegistration_thenCreatesSchoolAndDirector() {
        givenPendingRegistration();
        givenRegistrationExists();
        givenSchoolIsSaved();
        givenDirectorIsCreatedInCognito();

        whenApproveSchoolRegistration();

        thenRegistrationWasApproved();
        thenSchoolWasCreated();
        thenDirectorWasSaved();
    }

    @Test
    void givenMissingRegistration_whenApprovingRegistration_thenThrowsException() {
        givenApprovalRequest();
        givenRegistrationDoesNotExist();

        whenApproveSchoolRegistrationFails(IllegalArgumentException.class);

        thenErrorMessageIs("Registration not found");
        thenApprovalWasNotStarted();
    }

    @Test
    void givenRejectedRegistration_whenApprovingRegistration_thenThrowsException() {
        givenRegistrationWithStatus(SchoolRegistrationStatus.REJECTED);
        givenRegistrationExists();

        whenApproveSchoolRegistrationFails(IllegalStateException.class);

        thenErrorMessageIs("Registration is not pending");
        thenApprovalWasNotStarted();
    }

    @Test
    void givenExistingDirectorUsername_whenApprovingRegistration_thenRetriesWithSuffix() {
        givenPendingRegistration();
        givenRegistrationExists();
        givenSchoolIsSaved();
        givenDirectorUsernameAlreadyExists();
        givenDirectorIsCreatedInCognitoWithRetriedUsername();

        whenApproveSchoolRegistration();

        thenDirectorWasSavedWithRetriedCognitoSubject();
        thenRegistrationWasApproved();
    }

    private void givenPendingRegistration() {
        givenRegistrationWithStatus(SchoolRegistrationStatus.PENDING);
    }

    private void givenRegistrationWithStatus(SchoolRegistrationStatus status) {
        approvalRequest = new ApproveSchoolRegistrationRequest(randomUUID());
        registration = registration(approvalRequest.registrationId(), status);
    }

    private void givenApprovalRequest() {
        approvalRequest = new ApproveSchoolRegistrationRequest(randomUUID());
    }

    private void givenRegistrationExists() {
        when(registrationRepository.findById(approvalRequest.registrationId()))
                .thenReturn(Optional.of(registration));
    }

    private void givenRegistrationDoesNotExist() {
        when(registrationRepository.findById(approvalRequest.registrationId()))
                .thenReturn(Optional.empty());
    }

    private void givenSchoolIsSaved() {
        savedSchool = new Colegio();
        savedSchool.setId(randomUUID());
        when(colegioRepository.save(any(Colegio.class))).thenReturn(savedSchool);
    }

    private void givenDirectorIsCreatedInCognito() {
        when(cognitoService.createDirectorUser(
                DIRECTOR_USERNAME,
                DIRECTOR_EMAIL,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME))
                .thenReturn(new CreatedCognitoUser(DIRECTOR_COGNITO_SUB, "director-123"));
    }

    private void givenDirectorUsernameAlreadyExists() {
        when(cognitoService.createDirectorUser(
                DIRECTOR_USERNAME,
                DIRECTOR_EMAIL,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME))
                .thenThrow(new UsernameExistsException("exists"));
    }

    private void givenDirectorIsCreatedInCognitoWithRetriedUsername() {
        when(cognitoService.createDirectorUser(
                startsWith(DIRECTOR_USERNAME + "-"),
                eq(DIRECTOR_EMAIL),
                eq(DIRECTOR_FIRST_NAME),
                eq(DIRECTOR_LAST_NAME)))
                .thenReturn(new CreatedCognitoUser(
                        RETRY_DIRECTOR_COGNITO_SUB,
                        "director-retry"));
    }

    private void whenApproveSchoolRegistration() {
        approveSchoolRegistration.execute(approvalRequest);
    }

    private <T extends Throwable> void whenApproveSchoolRegistrationFails(
            Class<T> expectedException) {
        Executable execution = () -> approveSchoolRegistration.execute(approvalRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenRegistrationWasApproved() {
        assertEquals(SchoolRegistrationStatus.APPROVED, registration.getStatus());
        assertNotNull(registration.getUpdatedAt());
        verify(registrationRepository).save(registration);
    }

    private void thenSchoolWasCreated() {
        verify(colegioRepository).save(schoolCaptor.capture());
        Colegio school = schoolCaptor.getValue();
        assertEquals(SCHOOL_NAME, school.getNombre());
        assertEquals(SCHOOL_EMAIL, school.getEmail());
        assertEquals(SCHOOL_PHONE, school.getTelefono());
        assertEquals(SCHOOL_CUE, school.getCue());
        assertEquals(Boolean.TRUE, school.getActivo());
        assertNotNull(school.getCreadoEn());
        assertNotNull(school.getActualizadoEn());
    }

    private void thenDirectorWasSaved() {
        verify(usuarioRepository).save(directorCaptor.capture());
        Usuario director = directorCaptor.getValue();
        assertEquals(DIRECTOR_FIRST_NAME, director.getNombre());
        assertEquals(DIRECTOR_LAST_NAME, director.getApellido());
        assertEquals(DIRECTOR_EMAIL, director.getEmail());
        assertEquals(DIRECTOR_PHONE, director.getTelefono());
        assertEquals(DIRECTOR_DNI, director.getDni());
        assertEquals(DIRECTOR_COGNITO_SUB, director.getIdCognito());
        assertEquals(RolUsuario.DIRECTIVO_COLEGIO, director.getRol());
        assertEquals(savedSchool, director.getColegio());
        assertNotNull(director.getCreadoEn());
        assertNotNull(director.getActualizadoEn());
    }

    private void thenDirectorWasSavedWithRetriedCognitoSubject() {
        verify(usuarioRepository).save(directorCaptor.capture());
        assertEquals(RETRY_DIRECTOR_COGNITO_SUB, directorCaptor.getValue().getIdCognito());
    }

    private void thenApprovalWasNotStarted() {
        verify(colegioRepository, never()).save(any());
        verify(cognitoService, never()).createDirectorUser(any(), any(), any(), any());
        verify(usuarioRepository, never()).save(any());
        verify(registrationRepository, never()).save(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private SchoolRegistrationRequest registration(
            UUID registrationId,
            SchoolRegistrationStatus status) {
        SchoolRegistrationRequest registration = new SchoolRegistrationRequest();
        registration.setId(registrationId);
        registration.setStatus(status);
        registration.setSchoolName(SCHOOL_NAME);
        registration.setSchoolEmail(SCHOOL_EMAIL);
        registration.setSchoolPhone(SCHOOL_PHONE);
        registration.setSchoolCue(SCHOOL_CUE);
        registration.setDirectorFirstName(DIRECTOR_FIRST_NAME);
        registration.setDirectorLastName(DIRECTOR_LAST_NAME);
        registration.setDirectorEmail(DIRECTOR_EMAIL);
        registration.setDirectorPhone(DIRECTOR_PHONE);
        registration.setDirectorDni(DIRECTOR_DNI);
        registration.setDirectorUsername(DIRECTOR_USERNAME);
        return registration;
    }
}
