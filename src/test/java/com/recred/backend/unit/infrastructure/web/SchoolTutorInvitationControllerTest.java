package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.InviteTutorToSchool;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.infrastructure.web.SchoolTutorInvitationController;
import com.recred.backend.infrastructure.web.TutorInvitationImportResponse;
import com.recred.backend.infrastructure.web.TutorInvitationRequest;
import com.recred.backend.infrastructure.web.TutorInvitationResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolTutorInvitationControllerTest {

    @Mock
    private InviteTutorToSchool inviteTutorToSchool;

    @Mock
    private ImportTutorInvitationsFromCsv importTutorInvitationsFromCsv;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile file;

    private SchoolTutorInvitationController controller;

    @BeforeEach
    void setUp() {
        controller = new SchoolTutorInvitationController(
                inviteTutorToSchool,
                importTutorInvitationsFromCsv
        );
    }

    @Test
    void inviteShouldReturnCreatedAndMapCommand() {
        when(authentication.getName()).thenReturn("director-sub");
        TutorInvitation invitation = invitation();
        when(inviteTutorToSchool.execute(eq("director-sub"),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        invitation,
                        "https://front/invitaciones/tutor?token=abc",
                        "CREATED"
                ));

        ResponseEntity<TutorInvitationResponse> response = controller.invite(
                authentication,
                new TutorInvitationRequest(
                        "tutor@example.com",
                        "Ana",
                        "Gomez",
                        "1122334455"
                )
        );

        ArgumentCaptor<InviteTutorToSchool.TutorInvitationCommand> captor =
                ArgumentCaptor.forClass(InviteTutorToSchool.TutorInvitationCommand.class);
        verify(inviteTutorToSchool).execute(eq("director-sub"), captor.capture());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("tutor@example.com", captor.getValue().email());
        assertEquals(invitation.getId(), response.getBody().id());
        assertEquals("CREATED", response.getBody().result());
    }

    @Test
    void inviteShouldReturnOkWhenTutorIsAlreadyAssociated() {
        when(authentication.getName()).thenReturn("director-sub");
        when(inviteTutorToSchool.execute(eq("director-sub"),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InviteTutorToSchool.TutorInvitationResult(
                        null,
                        null,
                        "ALREADY_ASSOCIATED"
                ));

        ResponseEntity<TutorInvitationResponse> response = controller.invite(
                authentication,
                new TutorInvitationRequest(
                        "tutor@example.com",
                        null,
                        null,
                        null
                )
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ALREADY_ASSOCIATED", response.getBody().result());
    }

    @Test
    void importCsvShouldReadFileContentAndReturnCreated() throws Exception {
        when(authentication.getName()).thenReturn("director-sub");
        when(file.getBytes()).thenReturn("email\nana@example.com".getBytes(
                StandardCharsets.UTF_8));
        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result =
                new ImportTutorInvitationsFromCsv.TutorInvitationImportResult(
                        1,
                        1,
                        0,
                        0,
                        List.of(new ImportTutorInvitationsFromCsv.TutorInvitationImportError(
                                3,
                                "bad@example.com",
                                "Error"
                        ))
                );
        when(importTutorInvitationsFromCsv.execute(
                "director-sub",
                "email\nana@example.com")).thenReturn(result);

        ResponseEntity<TutorInvitationImportResponse> response =
                controller.importCsv(authentication, file);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().totalRows());
        assertEquals(1, response.getBody().createdInvitations());
        assertEquals(1, response.getBody().errors().size());
        assertEquals("bad@example.com", response.getBody().errors().get(0).email());
    }

    @Test
    void inviteShouldThrowWhenAuthenticationIsMissing() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.invite(null, new TutorInvitationRequest(
                        "tutor@example.com",
                        null,
                        null,
                        null
                )));
    }

    private TutorInvitation invitation() {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setSchool(school());
        invitation.setEmail("tutor@example.com");
        invitation.setFirstName("Ana");
        invitation.setLastName("Gomez");
        invitation.setPhone("1122334455");
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.of(2026, 7, 8, 10, 0));
        return invitation;
    }

    private Colegio school() {
        Colegio school = new Colegio();
        school.setId(UUID.randomUUID());
        school.setNombre("Colegio");
        return school;
    }
}
