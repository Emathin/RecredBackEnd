package com.recred.backend.unit.application;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.InviteTutorToSchool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
