package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sugerencias-consumo")
public class PurchaseSuggestController {

    private final PurchaseSuggestIA purchaseSuggestIA;
    private final RespondToAISuggestion respondToAISuggestion;
    private final ProcessPresentialPurchase processPresentialPurchase;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final ProductRepository productRepository;

    public PurchaseSuggestController(PurchaseSuggestIA purchaseSuggestIA,
                                     RespondToAISuggestion respondToAISuggestion,
                                     ProcessPresentialPurchase processPresentialPurchase,
                                     SugerenciaIARepository sugerenciaIARepository,
                                     ProductRepository productRepository) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.respondToAISuggestion = respondToAISuggestion;
        this.processPresentialPurchase = processPresentialPurchase;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/activa")
    public ResponseEntity<HealthySuggestionResponseDTO> obtenerSugerenciaActiva(
            @AuthenticationPrincipal Jwt jwt) {
        Optional<SugerenciaIAEntity> sugerenciaOpt = purchaseSuggestIA.execute(jwt.getSubject());

        return sugerenciaOpt
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{sugerenciaId}/comprar")
    public ResponseEntity<Void> comprarSugerencia(
            @PathVariable UUID sugerenciaId) {
        SugerenciaIAEntity sugerencia = sugerenciaIARepository.findById(sugerenciaId)
                .orElseThrow(() -> new RuntimeException("Sugerencia no encontrada"));

        // Marcar como aceptada
        respondToAISuggestion.execute(sugerenciaId, true);

        // Concretar la compra
        // TODO: ProcessPresentialPurchase es provisorio y en el futuro
        // se reemplazará por ProcessAnticipatedPurchase
        UUID alumnoId = sugerencia.getAlumnoEntity().getId();
        UUID productoId = sugerencia.getProductoEntity().getId();

        Producto producto = productRepository.findById(productoId)
                .orElseThrow(() -> new ProductNotFoundException(productoId));

        processPresentialPurchase.execute(alumnoId, Map.of(producto, 1));

        return ResponseEntity.ok().build();
    }

    private HealthySuggestionResponseDTO toDTO(SugerenciaIAEntity entity) {
        return HealthySuggestionResponseDTO.builder()
                .sugerenciaId(entity.getId())
                .titulo(entity.getTitulo())
                .mensaje(entity.getMensaje())
                .productoId(entity.getProductoEntity().getId())
                .productoNombre(entity.getProductoEntity().getNombre())
                .productoPrecio(entity.getProductoEntity().getPrecio())
                .build();
    }
}
