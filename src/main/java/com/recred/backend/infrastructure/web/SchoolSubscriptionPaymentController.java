package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateSchoolSubscriptionPayment;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentRequest;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/subscriptions")
public class SchoolSubscriptionPaymentController {

    private final CreateSchoolSubscriptionPayment createSchoolSubscriptionPayment;

    public SchoolSubscriptionPaymentController(
            CreateSchoolSubscriptionPayment createSchoolSubscriptionPayment
    ) {
        this.createSchoolSubscriptionPayment = createSchoolSubscriptionPayment;
    }

    @PostMapping("/school")
    public ResponseEntity<CreateSchoolSubscriptionPaymentResponse> createPayment(
            @RequestBody SchoolSubscriptionPaymentRequest request
    ) {
        return ResponseEntity.ok(createSchoolSubscriptionPayment.execute(
                toUseCaseRequest(request)));
    }

    private CreateSchoolSubscriptionPaymentRequest toUseCaseRequest(
            SchoolSubscriptionPaymentRequest request
    ) {
        return new CreateSchoolSubscriptionPaymentRequest(request.colegioId());
    }
}
