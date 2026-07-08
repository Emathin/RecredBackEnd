package com.recred.backend.unit.application;

import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectSchoolRegistrationTest {

    @Mock
    private SchoolRegistrationRequestRepository repository;

    private RejectSchoolRegistration useCase;

    private UUID registrationId;
    private SchoolRegistrationRequest registration;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new RejectSchoolRegistration(repository);
    }

    @Test
    void givenPendingRegistration_whenRejectingSchoolRegistration_thenRejectsRegistration() {
        givenSchoolRegistrationScenario(SchoolRegistrationStatus.PENDING);
        givenRegistrationExists();

        whenRejectingSchoolRegistration();

        thenRegistrationWasRejected();
        thenRegistrationWasSaved();
    }

    @Test
    void givenRegistrationDoesNotExist_whenRejectingSchoolRegistration_thenFails() {
        givenSchoolRegistrationScenario(SchoolRegistrationStatus.PENDING);
        givenRegistrationDoesNotExist();

        whenRejectingSchoolRegistrationFails();

        thenRegistrationNotFoundWasThrown();
        thenRegistrationWasNotSaved();
    }

    @Test
    void givenRegistrationIsNotPending_whenRejectingSchoolRegistration_thenFails() {
        givenSchoolRegistrationScenario(SchoolRegistrationStatus.APPROVED);
        givenRegistrationExists();

        whenRejectingSchoolRegistrationFails();

        thenRegistrationNotPendingWasThrown();
        thenRegistrationWasNotSaved();
    }

    private void givenSchoolRegistrationScenario(SchoolRegistrationStatus status) {
        registrationId = UUID.randomUUID();
        registration = new SchoolRegistrationRequest();
        registration.setId(registrationId);
        registration.setStatus(status);
    }

    private void givenRegistrationExists() {
        when(repository.findById(registrationId)).thenReturn(Optional.of(registration));
    }

    private void givenRegistrationDoesNotExist() {
        when(repository.findById(registrationId)).thenReturn(Optional.empty());
    }

    private void whenRejectingSchoolRegistration() {
        useCase.execute(new RejectSchoolRegistrationRequest(registrationId));
    }

    private void whenRejectingSchoolRegistrationFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenRejectingSchoolRegistration);
    }

    private void thenRegistrationWasRejected() {
        assertEquals(SchoolRegistrationStatus.REJECTED, registration.getStatus());
    }

    private void thenRegistrationWasSaved() {
        verify(repository).save(registration);
    }

    private void thenRegistrationNotFoundWasThrown() {
        assertTrue(thrownException instanceof IllegalArgumentException);
    }

    private void thenRegistrationNotPendingWasThrown() {
        assertTrue(thrownException instanceof IllegalStateException);
    }

    private void thenRegistrationWasNotSaved() {
        verify(repository, never()).save(any());
    }
}
