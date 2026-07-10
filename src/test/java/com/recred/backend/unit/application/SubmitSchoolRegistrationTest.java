package com.recred.backend.unit.application;

import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitSchoolRegistrationTest {

    @Mock
    private SchoolRegistrationRequestRepository registrationRepository;

    @Mock
    private ColegioRepository schoolRepository;

    @Mock
    private UsuarioRepository userRepository;

    private SubmitSchoolRegistration useCase;

    private SubmitSchoolRegistrationRequest request;
    private SchoolRegistrationRequest savedRegistration;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        useCase = new SubmitSchoolRegistration(
                registrationRepository,
                schoolRepository,
                userRepository
        );
    }

    @Test
    void givenAvailableSchoolAndDirector_whenSubmittingRegistration_thenSavesPendingRequest() {
        givenSchoolRegistrationRequest();
        givenDirectorEmailIsAvailable();

        whenSubmittingSchoolRegistration();

        thenSchoolDataWasSaved();
        thenDirectorDataWasSaved();
        thenRegistrationIsPending();
        thenSubmissionTimestampsWereSaved();
    }

    @Test
    void givenSchoolEmailExists_whenSubmittingRegistration_thenThrowsException() {
        givenSchoolRegistrationRequest();
        givenSchoolEmailAlreadyExists();

        whenSubmittingSchoolRegistrationAndCatchingException();

        thenExceptionIsThrownWithMessage("Email registrado");
    }

    @Test
    void givenDirectorEmailExists_whenSubmittingRegistration_thenThrowsException() {
        givenSchoolRegistrationRequest();
        givenDirectorEmailAlreadyExists();

        whenSubmittingSchoolRegistrationAndCatchingException();

        thenExceptionIsThrownWithMessage("Email registrado");
    }

    @Test
    void givenSchoolCueExists_whenSubmittingRegistration_thenThrowsException() {
        givenSchoolRegistrationRequest();
        givenSchoolCueAlreadyExists();

        whenSubmittingSchoolRegistrationAndCatchingException();

        thenExceptionIsThrownWithMessage("CUE registrado");
    }

    @Test
    void givenPendingRegistrationForEmail_whenSubmittingRegistration_thenThrowsException() {
        givenSchoolRegistrationRequest();
        givenPendingRegistrationForSchoolEmail();

        whenSubmittingSchoolRegistrationAndCatchingException();

        thenExceptionIsThrownWithMessage("Solicitud pendiente");
    }

    private void givenSchoolRegistrationRequest() {
        request = new SubmitSchoolRegistrationRequest(
                "School Name",
                "school@test.com",
                "123456789",
                "CUE-123",
                "Director",
                "LastName",
                "director@test.com",
                "987654321",
                "11223344",
                "directorUser123"
        );
    }

    private void givenDirectorEmailIsAvailable() {
        when(userRepository.findByEmail("director@test.com")).thenReturn(Optional.empty());
    }

    private void givenSchoolEmailAlreadyExists() {
        when(schoolRepository.existsByEmail("school@test.com")).thenReturn(true);
    }

    private void givenDirectorEmailAlreadyExists() {
        when(userRepository.findByEmail("director@test.com"))
                .thenReturn(Optional.of(new com.recred.backend.domain.models.Usuario()));
    }

    private void givenSchoolCueAlreadyExists() {
        when(schoolRepository.existsByCue("CUE-123")).thenReturn(true);
    }

    private void givenPendingRegistrationForSchoolEmail() {
        when(registrationRepository.existsBySchoolEmailAndStatus("school@test.com", 
                SchoolRegistrationStatus.PENDING)).thenReturn(true);
    }

    private void whenSubmittingSchoolRegistration() {
        useCase.execute(request);
        ArgumentCaptor<SchoolRegistrationRequest> captor =
                ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(registrationRepository).save(captor.capture());
        savedRegistration = captor.getValue();
    }

    private void whenSubmittingSchoolRegistrationAndCatchingException() {
        thrownException = assertThrows(IllegalArgumentException.class, 
                () -> useCase.execute(request));
    }

    private void thenSchoolDataWasSaved() {
        assertEquals("School Name", savedRegistration.getSchoolName());
        assertEquals("school@test.com", savedRegistration.getSchoolEmail());
        assertEquals("123456789", savedRegistration.getSchoolPhone());
        assertEquals("CUE-123", savedRegistration.getSchoolCue());
    }

    private void thenDirectorDataWasSaved() {
        assertEquals("Director", savedRegistration.getDirectorFirstName());
        assertEquals("LastName", savedRegistration.getDirectorLastName());
        assertEquals("director@test.com", savedRegistration.getDirectorEmail());
        assertEquals("987654321", savedRegistration.getDirectorPhone());
        assertEquals("11223344", savedRegistration.getDirectorDni());
        assertEquals("directorUser123", savedRegistration.getDirectorUsername());
    }

    private void thenRegistrationIsPending() {
        assertEquals(SchoolRegistrationStatus.PENDING, savedRegistration.getStatus());
    }

    private void thenSubmissionTimestampsWereSaved() {
        assertNotNull(savedRegistration.getCreatedAt());
        assertNotNull(savedRegistration.getUpdatedAt());
    }

    private void thenExceptionIsThrownWithMessage(String expectedMessage) {
        assertNotNull(thrownException);
        assertEquals(expectedMessage, thrownException.getMessage());
    }
}
