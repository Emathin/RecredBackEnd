package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.ImportTutorInvitationsFromCsvRequest;
import com.recred.backend.application.InviteTutorToSchool;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.infrastructure.web.SchoolTutorInvitationController;
import com.recred.backend.infrastructure.web.TutorInvitationImportResponse;
import com.recred.backend.infrastructure.web.TutorInvitationRequest;
import com.recred.backend.infrastructure.web.TutorInvitationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolTutorInvitationControllerTest {

    private static final String DIRECTOR_SUB = "director-sub";
    private static final String TUTOR_EMAIL = "tutor@example.com";
    private static final String CSV_CONTENT = "email\nana@example.com";
    private static final String INVITATION_LINK = "https://front/invitaciones/tutor?token=abc";
    private static final UUID INVITATION_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();

    @Mock
    private InviteTutorToSchool inviteTutorToSchool;

    @Mock
    private ImportTutorInvitationsFromCsv importTutorInvitationsFromCsv;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private SchoolTutorInvitationController controller;

    @Test
    void shouldReturnCreatedAndMapCommandWhenInviteIsCalled() {
        givenDirectorIsAuthenticated();
        givenInviteResult("CREATED", buildInvitation());

        ResponseEntity<TutorInvitationResponse> response = whenInviteIsCalled(buildRequest());

        thenInviteUseCaseWasCalledWithCorrectCommand();
        thenResponseIsCreated(response, "CREATED", INVITATION_ID);
    }

    @Test
    void shouldReturnOkWhenTutorIsAlreadyAssociated() {
        givenDirectorIsAuthenticated();
        givenInviteResult("ALREADY_ASSOCIATED", null);

        ResponseEntity<TutorInvitationResponse> response = whenInviteIsCalled(buildEmptyRequest());

        thenResponseIsOk(response, "ALREADY_ASSOCIATED");
    }

    @Test
    void shouldReadFileContentAndReturnCreatedWhenImportCsvIsCalled() throws Exception {
        givenDirectorIsAuthenticated();
        givenFileBytes(CSV_CONTENT);
        givenImportResultHasOneError();

        ResponseEntity<TutorInvitationImportResponse> response = whenImportCsvIsCalled();

        thenImportResponseIsCorrect(response);
    }

    @Test
    void shouldThrowExceptionWhenCsvFileIsEmpty() throws Exception {
        givenFileBytes("  ");

        Executable execution = this::whenImportCsvIsCalled;

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "El archivo CSV esta vacio");
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationIsMissing() {
        Executable execution = () -> controller.invite(null, buildRequest());

        thenThrowsAuthenticationException(execution);
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        Executable execution = () -> controller.invite(authentication, new TutorInvitationRequest(" ", "Ana", "Gomez", "111"));

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "El email es obligatorio");
    }

    @Test
    void shouldThrowExceptionWhenEmailIsInvalid() {
        Executable execution = () -> controller.invite(authentication, new TutorInvitationRequest("no-es-email", "Ana", "Gomez", "111"));
        
        thenThrowsIllegalArgumentExceptionWithMessage(execution, "El email no es valido");
    }



    private void givenDirectorIsAuthenticated() {
        when(authentication.getName()).thenReturn(DIRECTOR_SUB);
    }

    private void givenInviteResult(String status, TutorInvitation invitation) {
        when(inviteTutorToSchool.execute(any(com.recred.backend.application.InviteTutorToSchoolRequest.class)))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        invitation, INVITATION_LINK, status));
    }

    private void givenFileBytes(String content) throws Exception {
        when(file.getBytes()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
    }

    private void givenImportResultHasOneError() {
        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result =
                new ImportTutorInvitationsFromCsv.TutorInvitationImportResult(
                        1, 1, 0, 0,
                        List.of(new ImportTutorInvitationsFromCsv.TutorInvitationImportError(
                                3, "bad@example.com", "Error")));
                                
        when(importTutorInvitationsFromCsv.execute(
                new ImportTutorInvitationsFromCsvRequest(DIRECTOR_SUB, CSV_CONTENT)))
                .thenReturn(result);
    }



    private ResponseEntity<TutorInvitationResponse> whenInviteIsCalled(TutorInvitationRequest request) {
        return controller.invite(authentication, request);
    }

    private ResponseEntity<TutorInvitationImportResponse> whenImportCsvIsCalled() throws Exception {
        return controller.importCsv(authentication, file);
    }



    private void thenInviteUseCaseWasCalledWithCorrectCommand() {
        ArgumentCaptor<com.recred.backend.application.InviteTutorToSchoolRequest> captor =
                ArgumentCaptor.forClass(com.recred.backend.application.InviteTutorToSchoolRequest.class);
        verify(inviteTutorToSchool).execute(captor.capture());
        assertEquals(TUTOR_EMAIL, captor.getValue().email());
    }

    private void thenResponseIsCreated(
            ResponseEntity<TutorInvitationResponse> response, String expectedResult, UUID expectedId) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedId, response.getBody().id());
        assertEquals(expectedResult, response.getBody().result());
    }

    private void thenResponseIsOk(ResponseEntity<TutorInvitationResponse> response, String expectedResult) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResult, response.getBody().result());
    }

    private void thenImportResponseIsCorrect(ResponseEntity<TutorInvitationImportResponse> response) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().totalRows());
        assertEquals(1, response.getBody().createdInvitations());
        assertEquals(1, response.getBody().errors().size());
        assertEquals("bad@example.com", response.getBody().errors().get(0).email());
    }

    private void thenThrowsIllegalArgumentExceptionWithMessage(org.junit.jupiter.api.function.Executable execution, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals(message, exception.getMessage());
    }

    private void thenThrowsAuthenticationException(org.junit.jupiter.api.function.Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }



    private TutorInvitationRequest buildRequest() {
        return new TutorInvitationRequest(TUTOR_EMAIL, "Ana", "Gomez", "1122334455");
    }

    private TutorInvitationRequest buildEmptyRequest() {
        return new TutorInvitationRequest(TUTOR_EMAIL, null, null, null);
    }

    private TutorInvitation buildInvitation() {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(INVITATION_ID);
        invitation.setSchool(buildSchool());
        invitation.setEmail(TUTOR_EMAIL);
        invitation.setFirstName("Ana");
        invitation.setLastName("Gomez");
        invitation.setPhone("1122334455");
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.of(2026, 7, 8, 10, 0));
        return invitation;
    }

    private Colegio buildSchool() {
        Colegio school = new Colegio();
        school.setId(SCHOOL_ID);
        school.setNombre("Colegio");
        return school;
    }
}
