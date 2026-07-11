package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.application.GetSchoolRegistrations;
import com.recred.backend.application.GetSchoolRegistrationsResponse;
import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistrationRequest;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.infrastructure.web.AdminAccessGuard;
import com.recred.backend.infrastructure.web.SchoolRegistrationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolRegistrationControllerTest {

    private static final String SCHOOL_NAME = "Test School";
    private static final String SCHOOL_EMAIL = "school@test.com";
    private static final String SCHOOL_PHONE = "123456789";
    private static final String SCHOOL_CUE = "123456789";
    private static final String DIRECTOR_FIRST_NAME = "John";
    private static final String DIRECTOR_LAST_NAME = "Doe";
    private static final String DIRECTOR_EMAIL = "john@test.com";
    private static final String DIRECTOR_PHONE = "987654321";
    private static final String DIRECTOR_DNI = "12345678";
    private static final String DIRECTOR_USERNAME = "john-doe";
    private static final String PENDING_STATUS = "PENDING";

    @Mock private SubmitSchoolRegistration submitUseCase;
    @Mock private ApproveSchoolRegistration approveUseCase;
    @Mock private RejectSchoolRegistration rejectUseCase;
    @Mock private GetSchoolRegistrations getSchoolRegistrationsUseCase;
    @Mock private AdminAccessGuard adminAccessGuard;

    private SchoolRegistrationController controller;
    private TestingAuthenticationToken adminAuthentication;
    private UUID registrationId;

    @BeforeEach
    void setUp() {
        controller = new SchoolRegistrationController(
                submitUseCase,
                approveUseCase,
                rejectUseCase,
                getSchoolRegistrationsUseCase,
                adminAccessGuard
        );
        adminAuthentication = new TestingAuthenticationToken("admin-123", null);
        registrationId = randomUUID();
    }

    @Test
    void givenSchoolRegistrationSubmissionWhenSubmittingRegistrationThenDelegatesToUseCase() {
        SubmitSchoolRegistrationRequest request = givenSchoolRegistrationSubmission();

        ResponseEntity<Void> response = whenSubmitSchoolRegistration(request);

        thenResponseIsOk(response);
        thenSubmitRegistrationWasRequested();
    }

    @Test
    void givenInvalidSchoolRegistrationSubmissionWhenSubmittingRegistrationThenThrowsException() {
        SubmitSchoolRegistrationRequest request = givenInvalidSchoolRegistrationSubmission();
        givenSubmitUseCaseThrowsIllegalArgument(request);

        Executable execution = () -> whenSubmitSchoolRegistration(request);

        thenThrowsIllegalArgumentException(execution);
    }

    @Test
    void givenAdminUserWhenApprovingRegistrationThenDelegatesToUseCase() {
        ResponseEntity<Void> response = whenApproveSchoolRegistration();

        thenResponseIsOk(response);
        thenAdminAccessWasRequired();
        thenApproveRegistrationWasRequested();
    }

    @Test
    void givenAdminUserWhenRejectingRegistrationThenDelegatesToUseCase() {
        ResponseEntity<Void> response = whenRejectSchoolRegistration();

        thenResponseIsOk(response);
        thenAdminAccessWasRequired();
        thenRejectRegistrationWasRequested();
    }

    @Test
    void givenRegistrationsWithStatusWhenGettingRegistrationsThenReturnsRegistrationList() {
        givenRegistrationsWithStatus(PENDING_STATUS);

        ResponseEntity<List<GetSchoolRegistrationsResponse>> response = whenGetRegistrationsByStatus(PENDING_STATUS);

        thenRegistrationsAreReturned(response);
        thenAdminAccessWasRequired();
    }

    private SubmitSchoolRegistrationRequest givenSchoolRegistrationSubmission() {
        return new SubmitSchoolRegistrationRequest(
                SCHOOL_NAME,
                SCHOOL_EMAIL,
                SCHOOL_PHONE,
                SCHOOL_CUE,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME,
                DIRECTOR_EMAIL,
                DIRECTOR_PHONE,
                DIRECTOR_DNI,
                DIRECTOR_USERNAME
        );
    }

    private SubmitSchoolRegistrationRequest givenInvalidSchoolRegistrationSubmission() {
        return new SubmitSchoolRegistrationRequest(
                "",
                "not-an-email",
                "",
                "123",
                "",
                "",
                "",
                "",
                "123",
                ""
        );
    }

    private void givenRegistrationsWithStatus(String status) {
        GetSchoolRegistrationsResponse registration = new GetSchoolRegistrationsResponse(
                registrationId,
                SCHOOL_NAME,
                SCHOOL_EMAIL,
                SCHOOL_PHONE,
                SCHOOL_CUE,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME,
                DIRECTOR_EMAIL,
                DIRECTOR_PHONE,
                DIRECTOR_DNI,
                DIRECTOR_USERNAME,
                status,
                null
        );
        when(getSchoolRegistrationsUseCase.execute(status)).thenReturn(List.of(registration));
    }

    private ResponseEntity<Void> whenSubmitSchoolRegistration(SubmitSchoolRegistrationRequest request) {
        return controller.submitRegistration(request);
    }

    private ResponseEntity<Void> whenApproveSchoolRegistration() {
        return controller.approveRegistration(adminAuthentication, registrationId);
    }

    private ResponseEntity<Void> whenRejectSchoolRegistration() {
        return controller.rejectRegistration(adminAuthentication, registrationId);
    }

    private ResponseEntity<List<GetSchoolRegistrationsResponse>> whenGetRegistrationsByStatus(String status) {
        return controller.getRegistrations(adminAuthentication, status);
    }

    private void thenResponseIsOk(ResponseEntity<?> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void thenSubmitRegistrationWasRequested() {
        verify(submitUseCase).execute(new SubmitSchoolRegistrationRequest(
                SCHOOL_NAME,
                SCHOOL_EMAIL,
                SCHOOL_PHONE,
                SCHOOL_CUE,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME,
                DIRECTOR_EMAIL,
                DIRECTOR_PHONE,
                DIRECTOR_DNI,
                DIRECTOR_USERNAME
        ));
    }

    private void thenAdminAccessWasRequired() {
        verify(adminAccessGuard).requireAdminAccess(adminAuthentication);
    }

    private void thenApproveRegistrationWasRequested() {
        verify(approveUseCase).execute(new ApproveSchoolRegistrationRequest(registrationId));
    }

    private void thenRejectRegistrationWasRequested() {
        verify(rejectUseCase).execute(new RejectSchoolRegistrationRequest(registrationId));
    }

    private void thenRegistrationsAreReturned(ResponseEntity<List<GetSchoolRegistrationsResponse>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(registrationId, response.getBody().get(0).id());
        assertEquals(SCHOOL_NAME, response.getBody().get(0).schoolName());
        assertEquals(PENDING_STATUS, response.getBody().get(0).status());
    }

    private void givenSubmitUseCaseThrowsIllegalArgument(SubmitSchoolRegistrationRequest request) {
        when(submitUseCase.execute(request)).thenThrow(IllegalArgumentException.class);
    }

    private void thenThrowsIllegalArgumentException(Executable execution) {
        assertThrows(IllegalArgumentException.class, execution);
    }
}
