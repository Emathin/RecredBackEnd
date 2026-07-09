package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ActivateUserSubscription;
import com.recred.backend.application.ActivateUserSubscriptionRequest;
import com.recred.backend.application.ActivateUserSubscriptionResponse;
import com.recred.backend.application.CreateUserSubscriptionPayment;
import com.recred.backend.application.CreateUserSubscriptionPaymentRequest;
import com.recred.backend.application.CreateUserSubscriptionPaymentResponse;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments/subscriptions")
public class UserSubscriptionPaymentController {

    private final CreateUserSubscriptionPayment createUserSubscriptionPayment;
    private final ActivateUserSubscription activateUserSubscription;

    public UserSubscriptionPaymentController(
            CreateUserSubscriptionPayment createUserSubscriptionPayment,
            ActivateUserSubscription activateUserSubscription
    ) {
        this.createUserSubscriptionPayment = createUserSubscriptionPayment;
        this.activateUserSubscription = activateUserSubscription;
    }

    @PostMapping("/user")
    public ResponseEntity<CreateUserSubscriptionPaymentResponse> createPayment(
            @RequestBody UserSubscriptionPaymentRequest request
    ) {
        return ResponseEntity.ok(createUserSubscriptionPayment.execute(toUseCaseRequest(request)));
    }

    @PostMapping("/user/trial")
    public ResponseEntity<ActivateUserSubscriptionResponse> startTrial(
            @RequestBody UserSubscriptionTrialRequest request
    ) {
        ActivateUserSubscriptionRequest activationRequest = new ActivateUserSubscriptionRequest(
                request.usuarioId(),
                request.plan(),
                PeriodoSuscripcion.PRUEBA,
                BigDecimal.ZERO,
                null
        );
        return ResponseEntity.ok(activateUserSubscription.execute(activationRequest));
    }

    private CreateUserSubscriptionPaymentRequest toUseCaseRequest(
            UserSubscriptionPaymentRequest request
    ) {
        return new CreateUserSubscriptionPaymentRequest(
                request.usuarioId(),
                request.plan(),
                request.periodo()
        );
    }
}
