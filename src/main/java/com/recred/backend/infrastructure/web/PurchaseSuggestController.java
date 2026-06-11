package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final NotifyPurchaseSuggestions notifyPurchaseSuggestions;
    private final RespondToAISuggestion respondToAISuggestion;
    private final CreateAdvancePurchase createAdvancePurchase;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final UsuarioService usuarioService;

    public PurchaseSuggestController(PurchaseSuggestIA purchaseSuggestIA,
                                     NotifyPurchaseSuggestions notifyPurchaseSuggestions,
                                     RespondToAISuggestion respondToAISuggestion,
                                     CreateAdvancePurchase createAdvancePurchase,
                                     SugerenciaIARepository sugerenciaIARepository,
                                     UsuarioService usuarioService) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.notifyPurchaseSuggestions = notifyPurchaseSuggestions;
        this.respondToAISuggestion = respondToAISuggestion;
        this.createAdvancePurchase = createAdvancePurchase;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/activa")
    public ResponseEntity<HealthySuggestionResponseDTO> obtenerSugerenciaActiva(
            Authentication authentication,
            @RequestBody(required = false) TestSuggestionRequest testRequest) {
        String subject = (testRequest != null && testRequest.testCognitoId() != null) 
                ? testRequest.testCognitoId() 
                : cognitoSubject(authentication);
        
        Optional<SugerenciaIA> sugerenciaOpt = purchaseSuggestIA.execute(subject);

        return sugerenciaOpt
                .map(s -> {
                    Usuario usuario = usuarioService.obtenerUsuarioActual(subject);
                    return toDTO(s, usuario);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/test-notificacion")
    public ResponseEntity<Void> testNotificacion(
            Authentication authentication,
            @RequestBody(required = false) TestSuggestionRequest testRequest) {
        String subject = (testRequest != null && testRequest.testCognitoId() != null) 
                ? testRequest.testCognitoId() 
                : cognitoSubject(authentication);
        
        notifyPurchaseSuggestions.execute(subject);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comprar")
    public ResponseEntity<AdvancePurchaseResponse> comprarSugerencia(
            Authentication authentication,
            @RequestBody TestSuggestionRequest testRequest) {
        
        UUID sugerenciaId = testRequest.sugerenciaId();
        String subject = (testRequest.testCognitoId() != null) 
                ? testRequest.testCognitoId() 
                : cognitoSubject(authentication);

        SugerenciaIAEntity sugerencia = sugerenciaIARepository.findById(sugerenciaId)
                .orElseThrow(() -> new RuntimeException("Sugerencia no encontrada"));

        Usuario usuario = usuarioService.obtenerUsuarioActual(subject);

        // Marcar como aceptada
        respondToAISuggestion.execute(sugerenciaId, true);

        // Concretar la compra anticipada
        UUID alumnoId = sugerencia.getAlumnoEntity().getId();
        UUID productoId = sugerencia.getProductoEntity().getId();
        UUID buffetId = sugerencia.getBuffetEntity().getId();

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
                List.of(item),
                sugerenciaId,
                buffetId
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

    private HealthySuggestionResponseDTO toDTO(SugerenciaIA sugerencia, Usuario usuario) {
        return HealthySuggestionResponseDTO.builder()
                .sugerenciaId(sugerencia.getId())
                .titulo(sugerencia.getTitulo())
                .mensaje(sugerencia.getMensaje())
                .productoId(sugerencia.getProductoId())
                .type("PURCHASE_SUGGESTION")
                .rol(usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE")
                .build();
    }

    private HealthySuggestionResponseDTO toDTO(SugerenciaIAEntity entity, Usuario usuario) {
        return HealthySuggestionResponseDTO.builder()
                .sugerenciaId(entity.getId())
                .titulo(entity.getTitulo())
                .mensaje(entity.getMensaje())
                .productoId(entity.getProductoEntity().getId())
                .productoNombre(entity.getProductoEntity().getNombre())
                .productoPrecio(entity.getProductoEntity().getPrecio())
                .type("PURCHASE_SUGGESTION")
                .rol(usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE")
                .build();
    }
}
