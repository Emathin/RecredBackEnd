package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sugerencias-consumo")
public class PurchaseSuggestController {

    private final PurchaseSuggestIA purchaseSuggestIA;
    private final RespondToAISuggestion respondToAISuggestion;
    private final CreateAdvancePurchase createAdvancePurchase;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final UsuarioService usuarioService;

    public PurchaseSuggestController(PurchaseSuggestIA purchaseSuggestIA,
                                     RespondToAISuggestion respondToAISuggestion,
                                     CreateAdvancePurchase createAdvancePurchase,
                                     SugerenciaIARepository sugerenciaIARepository,
                                     UsuarioService usuarioService) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.respondToAISuggestion = respondToAISuggestion;
        this.createAdvancePurchase = createAdvancePurchase;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/activa")
    public ResponseEntity<HealthySuggestionResponseDTO> obtenerSugerenciaActiva(
            Authentication authentication) {
        Optional<SugerenciaIAEntity> sugerenciaOpt =
                purchaseSuggestIA.execute(cognitoSubject(authentication));

        return sugerenciaOpt
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{sugerenciaId}/comprar")
    public ResponseEntity<AdvancePurchaseResponse> comprarSugerencia(
            @PathVariable UUID sugerenciaId,
            Authentication authentication) {
        SugerenciaIAEntity sugerencia = sugerenciaIARepository.findById(sugerenciaId)
                .orElseThrow(() -> new RuntimeException("Sugerencia no encontrada"));

        Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoSubject(authentication));

        // Marcar como aceptada
        respondToAISuggestion.execute(sugerenciaId, true);

        // Concretar la compra anticipada
        UUID alumnoId = sugerencia.getAlumnoEntity().getId();
        UUID productoId = sugerencia.getProductoEntity().getId();

        BuyerType buyerType = usuario.getRol() == RolUsuario.ALUMNO
                ? BuyerType.STUDENT
                : BuyerType.TUTOR;

        AdvancePurchaseRequest.ItemRequest item =
                new AdvancePurchaseRequest.ItemRequest(productoId, 1);

        AdvancePurchaseRequest.OrderRequest order = new AdvancePurchaseRequest.OrderRequest(
                alumnoId,
                usuario.getId(),
                buyerType,
                LocalDate.now().toString(),
                RecessTime.AFTER_HOURS,
                List.of(item)
        );

        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(order));

        return ResponseEntity.ok(createAdvancePurchase.execute(request));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
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
