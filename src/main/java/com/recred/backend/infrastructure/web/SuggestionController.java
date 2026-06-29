package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByNotificationIdRequest;
import com.recred.backend.application.GetSuggestionResponse;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.GetSuggestionByTypeRequest;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SuggestionController {

    private final GetSuggestionByNotificationId getSuggestionByNotificationId;
    private final GetSuggestionByType getSuggestionByType;

    public SuggestionController(GetSuggestionByNotificationId getSuggestionByNotificationId,
                                GetSuggestionByType getSuggestionByType) {
        this.getSuggestionByNotificationId = getSuggestionByNotificationId;
        this.getSuggestionByType = getSuggestionByType;
    }

    @GetMapping("/sugerencias/por-notificacion/{idNotificacion}")
    public ResponseEntity<GetSuggestionResponse> getSuggestionByNotificationId(
            Authentication authentication,
            @PathVariable UUID idNotificacion) {
        GetSuggestionByNotificationIdRequest request = new GetSuggestionByNotificationIdRequest(
                idNotificacion, cognitoSubject(authentication));
        GetSuggestionResponse response = getSuggestionByNotificationId.execute(request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sugerencias/agregar-producto")
    public ResponseEntity<List<GetSuggestionResponse>> getSuggestionsByBuffetIdAndType(
            Authentication authentication) {
        GetSuggestionByTypeRequest request = new GetSuggestionByTypeRequest(
                cognitoSubject(authentication), TipoSugerenciaIA.AGREGAR_PRODUCTO);
        return ResponseEntity.ok(getSuggestionByType.execute(request));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
