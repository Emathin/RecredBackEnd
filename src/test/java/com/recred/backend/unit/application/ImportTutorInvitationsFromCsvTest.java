package com.recred.backend.unit.application;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.ImportTutorInvitationsFromCsvRequest;
import com.recred.backend.application.InviteTutorToSchool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportTutorInvitationsFromCsvTest {

    private static final String DIRECTOR_SUBJECT = "director-sub";
    private static final String DUPLICATE_EMAIL_CSV = """
            email,nombre,apellido,telefono
            parent@test.com,Ana,Gomez,1111
            parent@test.com,Ana,Gomez,1111
            """;
    private static final String MISSING_EMAIL_COLUMN_CSV = "nombre,apellido\nAna,Gomez";
    private static final String MIXED_RESULTS_CSV = """
            email,firstName,lastName,phone
            created@test.com,Ana,Gomez,1111
            resent@test.com,Juan,Perez,2222
            associated@test.com,Lu,Diaz,3333
            invalid-email,Bad,Mail,4444
            """;
    private static final String QUOTED_VALUES_CSV = "email,nombre\n\"parent@test.com\",\"Ana \"\"Nani\"\"\"\n\n";

    @Mock
    private InviteTutorToSchool inviteTutorToSchool;

    @InjectMocks
    private ImportTutorInvitationsFromCsv useCase;

    @Test
    void shouldReportDuplicateEmailAndContinue() {
        givenInvitationIsCreated();

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result = whenExecute(DUPLICATE_EMAIL_CSV);

        thenResultHasDuplicateEmailError(result);
    }

    @Test
    void shouldThrowErrorWhenHeaderIsMissingEmailColumn() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> whenExecute(MISSING_EMAIL_COLUMN_CSV));

        assertEquals("El archivo CSV debe incluir una columna email", exception.getMessage());
    }

    @Test
    void shouldCountEachResultStatusCorrectly() {
        givenMixedInvitationResults();

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result = whenExecute(MIXED_RESULTS_CSV);

        thenResultCountsAreCorrect(result);
        thenResultHasInvalidEmailError(result);
    }

    @Test
    void shouldParseEscapedQuotesAndMissingColumns() {
        givenInvitationIsCreated();

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result = whenExecute(QUOTED_VALUES_CSV);

        thenResultHasNoErrorsAndCreatedOne(result);
    }

    private void givenInvitationIsCreated() {
        when(inviteTutorToSchool.execute(
                any(com.recred.backend.application.InviteTutorToSchoolRequest.class)))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null,
                        null,
                        "CREATED"));
    }

    private void givenMixedInvitationResults() {
        when(inviteTutorToSchool.execute(
                any(com.recred.backend.application.InviteTutorToSchoolRequest.class)))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null, null, "CREATED_EMAIL_ERROR"))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null, null, "RESENT_EMAIL_ERROR"))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null, null, "ALREADY_ASSOCIATED"));
    }

    private ImportTutorInvitationsFromCsv.TutorInvitationImportResult whenExecute(String csv) {
        ImportTutorInvitationsFromCsvRequest request = 
                new ImportTutorInvitationsFromCsvRequest(DIRECTOR_SUBJECT, csv);
        return useCase.execute(request);
    }

    private void thenResultHasDuplicateEmailError(ImportTutorInvitationsFromCsv.TutorInvitationImportResult result) {
        assertEquals(2, result.totalRows());
        assertEquals(1, result.createdInvitations());
        assertEquals(1, result.errors().size());
        assertEquals("El email esta duplicado en el archivo", result.errors().get(0).message());
    }

    private void thenResultCountsAreCorrect(ImportTutorInvitationsFromCsv.TutorInvitationImportResult result) {
        assertEquals(4, result.totalRows());
        assertEquals(1, result.createdInvitations());
        assertEquals(1, result.resentInvitations());
        assertEquals(1, result.alreadyAssociated());
    }

    private void thenResultHasInvalidEmailError(ImportTutorInvitationsFromCsv.TutorInvitationImportResult result) {
        assertEquals(1, result.errors().size());
        assertEquals("invalid-email", result.errors().get(0).email());
        assertEquals("El email no es valido", result.errors().get(0).message());
    }

    private void thenResultHasNoErrorsAndCreatedOne(ImportTutorInvitationsFromCsv.TutorInvitationImportResult result) {
        assertEquals(2, result.totalRows());
        assertEquals(1, result.createdInvitations());
        assertEquals(0, result.errors().size());
    }
}
