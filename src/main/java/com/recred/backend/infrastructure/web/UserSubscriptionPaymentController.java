package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateUserSubscriptionPayment;
import com.recred.backend.application.CreateUserSubscriptionPaymentRequest;
import com.recred.backend.application.CreateUserSubscriptionPaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/subscriptions")
public class UserSubscriptionPaymentController {

    private final CreateUserSubscriptionPayment createUserSubscriptionPayment;

    public UserSubscriptionPaymentController(
            CreateUserSubscriptionPayment createUserSubscriptionPayment
    ) {
        this.createUserSubscriptionPayment = createUserSubscriptionPayment;
    }

    @PostMapping("/user")
    public ResponseEntity<CreateUserSubscriptionPaymentResponse> createPayment(
            @RequestBody UserSubscriptionPaymentRequest request
    ) {
        return ResponseEntity.ok(createUserSubscriptionPayment.execute(toUseCaseRequest(request)));
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
