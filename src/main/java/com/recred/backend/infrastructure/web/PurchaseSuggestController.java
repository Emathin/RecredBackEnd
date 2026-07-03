package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.NotifyPurchaseSuggestionsRequest;
import com.recred.backend.application.NotifyPurchaseSuggestionsResponse;
import com.recred.backend.application.PushNotificationResponse;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sugerencias-consumo")
public class PurchaseSuggestController {

    private final NotifyPurchaseSuggestions notifyPurchaseSuggestions;
    private final RespondToAISuggestion respondToAISuggestion;
    private final CreateAdvancePurchase createAdvancePurchase;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final UsuarioService usuarioService;
    private final TutorRepository tutorRepository;

    public PurchaseSuggestController(NotifyPurchaseSuggestions notifyPurchaseSuggestions,
                                     RespondToAISuggestion respondToAISuggestion,
                                     CreateAdvancePurchase createAdvancePurchase,
                                     SugerenciaIARepository sugerenciaIARepository,
                                     UsuarioService usuarioService,
                                     TutorRepository tutorRepository) {
        this.notifyPurchaseSuggestions = notifyPurchaseSuggestions;
        this.respondToAISuggestion = respondToAISuggestion;
        this.createAdvancePurchase = createAdvancePurchase;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.usuarioService = usuarioService;
        this.tutorRepository = tutorRepository;
    }

    @PostMapping("/test-notificacion")
    public ResponseEntity<PushNotificationResponse> testNotification(
            Authentication authentication,
            @RequestBody(required = false) TestSuggestionRequest testRequest) {
        String subject = (testRequest != null && testRequest.testCognitoId() != null) 
                ? testRequest.testCognitoId() 
                : cognitoSubject(authentication);
        
        NotifyPurchaseSuggestionsResponse response = notifyPurchaseSuggestions.execute(
                new NotifyPurchaseSuggestionsRequest(subject)
        );
        return response.notification() != null
                ? ResponseEntity.ok(response.notification())
                : ResponseEntity.noContent().build();
    }

    @PostMapping("/comprar")
    public ResponseEntity<AdvancePurchaseResponse> buySuggestion(
            Authentication authentication,
            @RequestBody TestSuggestionRequest testRequest) {
        
        UUID idSuggestion = testRequest.sugerenciaId();
        String subject = (testRequest.testCognitoId() != null) 
                ? testRequest.testCognitoId() 
                : cognitoSubject(authentication);

        SugerenciaIAEntity suggestion = sugerenciaIARepository.findById(idSuggestion)
                .orElseThrow(() -> new RuntimeException("Sugerencia no encontrada"));

        Usuario usuario = usuarioService.obtenerUsuarioActual(subject);

        respondToAISuggestion.execute(new RespondToAISuggestionRequest(idSuggestion, true));

        UUID idStudent = suggestion.getAlumnoEntity().getId();
        UUID idProduct = suggestion.getProductoEntity().getId();
        UUID idBuffet = suggestion.getBuffetEntity().getId();

        UUID idTutor = tutorRepository.findByAlumnoId(idStudent).stream()
                .findFirst()
                .map(t -> t.getId())
                .orElse(null);

        BuyerType buyerType = usuario.getRol() == RolUsuario.ALUMNO
                ? BuyerType.STUDENT
                : BuyerType.TUTOR;

        CreateAdvancePurchaseRequest.ItemRequest item =
                new CreateAdvancePurchaseRequest.ItemRequest(idProduct, 1);

        CreateAdvancePurchaseRequest.OrderRequest order =
                new CreateAdvancePurchaseRequest.OrderRequest(
                        idStudent,
                        usuario.getId(),
                        buyerType,
                        idBuffet,
                        LocalDate.now().toString(),
                        RecessTime.AFTER_HOURS,
                        List.of(item),
                        idSuggestion,
                        idTutor
                );


        CreateAdvancePurchaseRequest request =
                new CreateAdvancePurchaseRequest(List.of(order));

        CreateAdvancePurchaseResponse useCaseResponse =
                createAdvancePurchase.execute(request.withCognitoSub(subject));

        return ResponseEntity.ok(new AdvancePurchaseResponse(
                useCaseResponse.orderId(),
                useCaseResponse.status(),
                useCaseResponse.total(),
                useCaseResponse.codes()
        ));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
