package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.AskSchoolAssistantRequest;
import com.recred.backend.application.SchoolAssistantConversations;
import com.recred.backend.domain.models.SchoolAssistantRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchoolAssistantController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final AskSchoolAssistant askSchoolAssistant;
    private final SchoolAssistantConversations conversations;

    public SchoolAssistantController(
            AskSchoolAssistant askSchoolAssistant,
            SchoolAssistantConversations conversations
    ) {
        this.askSchoolAssistant = askSchoolAssistant;
        this.conversations = conversations;
    }

    @PostMapping("/api/v1/ia/alumnos/me/asistente/mensajes")
    public SchoolAssistantResponse askStudent(
            Authentication authentication,
            @RequestBody SchoolAssistantRequest request
    ) {
        return SchoolAssistantResponse.fromApplication(askSchoolAssistant.execute(
                assistantRequest(authentication, request, SchoolAssistantRole.ALUMNO)
        ));
    }

    @GetMapping("/api/v1/ia/alumnos/me/asistente/sesiones")
    public List<SchoolAssistantSessionResponse> studentSessions(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return conversations.findStudentSessions(cognitoSubject(authentication), page, size)
                .stream()
                .map(SchoolAssistantSessionResponse::fromDomain)
                .toList();
    }

    @GetMapping("/api/v1/ia/alumnos/me/asistente/sesiones/{sesionId}/mensajes")
    public List<SchoolAssistantMessageResponse> studentMessages(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return conversations.findStudentMessages(
                        cognitoSubject(authentication),
                        sessionId,
                        page,
                        size
                )
                .stream()
                .map(SchoolAssistantMessageResponse::fromDomain)
                .toList();
    }

    @PatchMapping("/api/v1/ia/alumnos/me/asistente/sesiones/{sesionId}/cerrar")
    public ResponseEntity<Void> closeStudentSession(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId
    ) {
        conversations.closeStudentSession(cognitoSubject(authentication), sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/ia/alumnos/me/asistente/sesiones/{sesionId}")
    public ResponseEntity<Void> deleteStudentSession(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId
    ) {
        conversations.deleteStudentSession(cognitoSubject(authentication), sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/ia/tutores/me/asistente/mensajes")
    public SchoolAssistantResponse askTutor(
            Authentication authentication,
            @RequestBody SchoolAssistantRequest request
    ) {
        return SchoolAssistantResponse.fromApplication(askSchoolAssistant.execute(
                assistantRequest(authentication, request, SchoolAssistantRole.TUTOR)
        ));
    }

    @GetMapping("/api/v1/ia/tutores/me/asistente/sesiones")
    public List<SchoolAssistantSessionResponse> tutorSessions(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return conversations.findTutorSessions(cognitoSubject(authentication), page, size)
                .stream()
                .map(SchoolAssistantSessionResponse::fromDomain)
                .toList();
    }

    @GetMapping("/api/v1/ia/tutores/me/asistente/sesiones/{sesionId}/mensajes")
    public List<SchoolAssistantMessageResponse> tutorMessages(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return conversations.findTutorMessages(
                        cognitoSubject(authentication),
                        sessionId,
                        page,
                        size
                )
                .stream()
                .map(SchoolAssistantMessageResponse::fromDomain)
                .toList();
    }

    @PatchMapping("/api/v1/ia/tutores/me/asistente/sesiones/{sesionId}/cerrar")
    public ResponseEntity<Void> closeTutorSession(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId
    ) {
        conversations.closeTutorSession(cognitoSubject(authentication), sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/ia/tutores/me/asistente/sesiones/{sesionId}")
    public ResponseEntity<Void> deleteTutorSession(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId
    ) {
        conversations.deleteTutorSession(cognitoSubject(authentication), sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/ia/kiosqueros/me/asistente/mensajes")
    public SchoolAssistantResponse askKiosquero(
            Authentication authentication,
            @RequestBody SchoolAssistantRequest request
    ) {
        return SchoolAssistantResponse.fromApplication(askSchoolAssistant.execute(
                assistantRequest(authentication, request, SchoolAssistantRole.KIOSQUERO)
        ));
    }

    @GetMapping("/api/v1/ia/kiosqueros/me/asistente/sesiones")
    public List<SchoolAssistantSessionResponse> kiosqueroSessions(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return conversations.findKiosqueroSessions(cognitoSubject(authentication), page, size)
                .stream()
                .map(SchoolAssistantSessionResponse::fromDomain)
                .toList();
    }

    @GetMapping("/api/v1/ia/kiosqueros/me/asistente/sesiones/{sesionId}/mensajes")
    public List<SchoolAssistantMessageResponse> kiosqueroMessages(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return conversations.findKiosqueroMessages(
                        cognitoSubject(authentication),
                        sessionId,
                        page,
                        size
                )
                .stream()
                .map(SchoolAssistantMessageResponse::fromDomain)
                .toList();
    }

    @PatchMapping("/api/v1/ia/kiosqueros/me/asistente/sesiones/{sesionId}/cerrar")
    public ResponseEntity<Void> closeKiosqueroSession(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId
    ) {
        conversations.closeKiosqueroSession(cognitoSubject(authentication), sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/ia/kiosqueros/me/asistente/sesiones/{sesionId}")
    public ResponseEntity<Void> deleteKiosqueroSession(
            Authentication authentication,
            @PathVariable("sesionId") UUID sessionId
    ) {
        conversations.deleteKiosqueroSession(cognitoSubject(authentication), sessionId);
        return ResponseEntity.noContent().build();
    }

    private AskSchoolAssistantRequest assistantRequest(
            Authentication authentication,
            SchoolAssistantRequest request,
            SchoolAssistantRole role
    ) {
        return new AskSchoolAssistantRequest(
                cognitoSubject(authentication),
                role,
                request.sessionId(),
                request.message()
        );
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
