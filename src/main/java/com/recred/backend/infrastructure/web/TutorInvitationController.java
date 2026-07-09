package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AcceptTutorInvitation;
import com.recred.backend.application.AcceptTutorInvitationRequest;
import com.recred.backend.application.AcceptTutorInvitationResponse;
import com.recred.backend.application.GetTutorInvitation;
import com.recred.backend.application.PrepareTutorInvitationAccount;
import com.recred.backend.application.PrepareTutorInvitationAccountRequest;
import com.recred.backend.application.PrepareTutorInvitationAccountResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitaciones/tutor")
public class TutorInvitationController {

    private final GetTutorInvitation getTutorInvitation;
    private final AcceptTutorInvitation acceptTutorInvitation;
    private final PrepareTutorInvitationAccount prepareTutorInvitationAccount;

    public TutorInvitationController(GetTutorInvitation getTutorInvitation,
                                     AcceptTutorInvitation acceptTutorInvitation,
                                     PrepareTutorInvitationAccount prepareTutorInvitationAccount) {
        this.getTutorInvitation = getTutorInvitation;
        this.acceptTutorInvitation = acceptTutorInvitation;
        this.prepareTutorInvitationAccount = prepareTutorInvitationAccount;
    }

    @GetMapping("/{token}")
    public ResponseEntity<TutorInvitationResponse> getInvitation(@PathVariable String token) {
        var response = getTutorInvitation.execute(token);
        return ResponseEntity.ok(
                TutorInvitationResponse.fromApplication(response, null, "VALIDATED"));
    }

    @PostMapping("/{token}/preparar-cuenta")
    public ResponseEntity<PrepareTutorInvitationAccountResponse> prepareAccount(
            @PathVariable String token,
            @Valid @RequestBody(required = false) PrepareTutorInvitationAccountRequestDTO request) {
        return ResponseEntity.ok(prepareTutorInvitationAccount.execute(
                new PrepareTutorInvitationAccountRequest(
                        token,
                        request != null ? request.username() : null)));
    }

    @PostMapping("/{token}/aceptar")
    public ResponseEntity<TutorInvitationResponse> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt) {
        AcceptTutorInvitationResponse invitation = acceptTutorInvitation.execute(
                new AcceptTutorInvitationRequest(
                        token,
                        jwt.getSubject(),
                        jwt.getClaimAsString("email"),
                        firstNonBlank(
                                jwt.getClaimAsString("given_name"),
                                jwt.getClaimAsString("name")),
                        jwt.getClaimAsString("family_name"),
                        jwt.getClaimAsString("phone_number"),
                        jwt.getClaimAsString("custom:document_number")
                ));
        return ResponseEntity.ok(TutorInvitationResponse.fromApplication(
                invitation, null, "ACCEPTED"));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
