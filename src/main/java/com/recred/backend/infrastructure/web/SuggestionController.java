package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetSuggestionByNotificationId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SuggestionController {

    private final GetSuggestionByNotificationId getSuggestionByNotificationId;

    public SuggestionController(GetSuggestionByNotificationId getSuggestionByNotificationId) {
        this.getSuggestionByNotificationId = getSuggestionByNotificationId;
    }

    @GetMapping("/sugerencias/por-notificacion/{idNotificacion}")
    public ResponseEntity<SuggestionResponse> getSuggestionByNotificationId(
            @PathVariable UUID idNotificacion) {
        return ResponseEntity.ok(SuggestionResponse.fromDomain(
                getSuggestionByNotificationId.execute(idNotificacion)));
    }
}
