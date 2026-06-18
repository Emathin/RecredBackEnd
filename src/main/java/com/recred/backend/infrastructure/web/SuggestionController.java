package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetSuggestionByNotificationId;
import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    public ResponseEntity<SuggestionResponse> getSuggestionByNotificationId(
            @PathVariable UUID idNotificacion) {
        return ResponseEntity.ok(SuggestionResponse.fromDomain(
                getSuggestionByNotificationId.execute(idNotificacion)));
    }

    @GetMapping("/sugerencias/agregar-producto/{idUsuario}")
    public ResponseEntity<List<SuggestionResponse>> getSuggestionsByBuffetIdAndType(
            @PathVariable UUID idUsuario){
        return ResponseEntity.ok(
                getSuggestionByType.execute(
                        idUsuario, TipoSugerenciaIA.AGREGAR_PRODUCTO)
                .stream()
                .map(SuggestionResponse::fromDomain)
                .toList());
    }
}
