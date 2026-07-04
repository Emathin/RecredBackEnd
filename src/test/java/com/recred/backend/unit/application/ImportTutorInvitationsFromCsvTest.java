package com.recred.backend.unit.application;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.InviteTutorToSchool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportTutorInvitationsFromCsvTest {

    private static final String DIRECTOR_SUBJECT = "director-sub";

    @Mock
    private InviteTutorToSchool inviteTutorToSchool;

    private ImportTutorInvitationsFromCsv useCase;

    @BeforeEach
    void setUp() {
        useCase = new ImportTutorInvitationsFromCsv(inviteTutorToSchool);
    }

    @Test
    void givenCsvWithDuplicateEmail_whenImporting_thenReportsDuplicateAndContinues() {
        givenInvitationIsCreated();

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result = useCase.execute(
                DIRECTOR_SUBJECT,
                """
                        email,nombre,apellido,telefono
                        parent@test.com,Ana,Gomez,1111
                        parent@test.com,Ana,Gomez,1111
                        """);

        assertEquals(2, result.totalRows());
        assertEquals(1, result.createdInvitations());
        assertEquals(1, result.errors().size());
        assertEquals("El email esta duplicado en el archivo",
                result.errors().get(0).message());
    }

    @Test
    void givenBlankCsv_whenImporting_thenThrowsEmptyFileError() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(DIRECTOR_SUBJECT, " "));

        assertEquals("El archivo CSV esta vacio", exception.getMessage());
    }

    @Test
    void givenHeaderWithoutEmail_whenImporting_thenThrowsHeaderError() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(DIRECTOR_SUBJECT, "nombre,apellido\nAna,Gomez"));

        assertEquals("El archivo CSV debe incluir una columna email", exception.getMessage());
    }

    @Test
    void givenCsvWithDifferentResultStatuses_whenImporting_thenCountsEachResult() {
        when(inviteTutorToSchool.execute(
                eq(DIRECTOR_SUBJECT),
                any(InviteTutorToSchool.TutorInvitationCommand.class)))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null,
                        null,
                        "CREATED_EMAIL_ERROR"))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null,
                        null,
                        "RESENT_EMAIL_ERROR"))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null,
                        null,
                        "ALREADY_ASSOCIATED"))
                .thenThrow(new IllegalArgumentException("El email no es valido"));

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result = useCase.execute(
                DIRECTOR_SUBJECT,
                """
                        email,firstName,lastName,phone
                        created@test.com,Ana,Gomez,1111
                        resent@test.com,Juan,Perez,2222
                        associated@test.com,Lu,Diaz,3333
                        invalid@test.com,Bad,Mail,4444
                        """);

        assertEquals(4, result.totalRows());
        assertEquals(1, result.createdInvitations());
        assertEquals(1, result.resentInvitations());
        assertEquals(1, result.alreadyAssociated());
        assertEquals(1, result.errors().size());
        assertEquals("invalid@test.com", result.errors().get(0).email());
        assertEquals("El email no es valido", result.errors().get(0).message());
    }

    @Test
    void givenQuotedCsvValues_whenImporting_thenParsesEscapedQuotesAndMissingColumns() {
        givenInvitationIsCreated();

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result = useCase.execute(
                DIRECTOR_SUBJECT,
                "email,nombre\n\"parent@test.com\",\"Ana \"\"Nani\"\"\"\n\n");

        assertEquals(2, result.totalRows());
        assertEquals(1, result.createdInvitations());
        assertEquals(0, result.errors().size());
    }

    private void givenInvitationIsCreated() {
        when(inviteTutorToSchool.execute(
                eq(DIRECTOR_SUBJECT),
                any(InviteTutorToSchool.TutorInvitationCommand.class)))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null,
                        null,
                        "CREATED"));
    }
}
