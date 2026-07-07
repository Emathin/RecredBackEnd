package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ImportTutorInvitationsFromCsv;
import com.recred.backend.application.ImportTutorInvitationsFromCsvRequest;
import com.recred.backend.application.InviteTutorToSchool;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/colegio/tutores/invitaciones")
public class SchoolTutorInvitationController {

    private final InviteTutorToSchool inviteTutorToSchool;
    private final ImportTutorInvitationsFromCsv importTutorInvitationsFromCsv;

    public SchoolTutorInvitationController(
            InviteTutorToSchool inviteTutorToSchool,
            ImportTutorInvitationsFromCsv importTutorInvitationsFromCsv) {
        this.inviteTutorToSchool = inviteTutorToSchool;
        this.importTutorInvitationsFromCsv = importTutorInvitationsFromCsv;
    }

    @PostMapping
    public ResponseEntity<TutorInvitationResponse> invite(
            Authentication authentication,
            @RequestBody TutorInvitationRequest request) {
        InviteTutorToSchool.TutorInvitationResult result = inviteTutorToSchool.execute(
                cognitoSubject(authentication),
                new InviteTutorToSchool.TutorInvitationCommand(
                        request.email(),
                        request.firstName(),
                        request.lastName(),
                        request.phone()
                )
        );

        HttpStatus status = "ALREADY_ASSOCIATED".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(TutorInvitationResponse.fromDomain(
                        result.invitation(),
                        result.invitationLink(),
                        result.status()));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TutorInvitationImportResponse> importCsv(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.isBlank()) {
            throw new IllegalArgumentException("El archivo CSV esta vacio");
        }

        ImportTutorInvitationsFromCsv.TutorInvitationImportResult result =
                importTutorInvitationsFromCsv.execute(
                        new ImportTutorInvitationsFromCsvRequest(
                                cognitoSubject(authentication),
                                content
                        )
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TutorInvitationImportResponse.fromResult(result));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
