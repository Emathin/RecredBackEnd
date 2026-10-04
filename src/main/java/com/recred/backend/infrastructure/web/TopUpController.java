package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GenerateTopUpLinkRequest;
import com.recred.backend.application.GenerateTopUpLinkResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class TopUpController {

    private final GenerateTopUpLink generateTopUpLink;
    private final com.recred.backend.application.ScheduleWalletTopUp scheduleWalletTopUp;

    public TopUpController(
            GenerateTopUpLink generateTopUpLink,
            com.recred.backend.application.ScheduleWalletTopUp scheduleWalletTopUp
    ) {
        this.generateTopUpLink = generateTopUpLink;
        this.scheduleWalletTopUp = scheduleWalletTopUp;
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, String>> requestTopUpLink(@RequestBody TopUpRequest request) {
        GenerateTopUpLinkRequest topUpRequest = new GenerateTopUpLinkRequest(
                request.studentId(),
                request.amount()
        );

        GenerateTopUpLinkResponse response = generateTopUpLink.execute(topUpRequest);

        return ResponseEntity.ok(Map.of("paymentUrl", response.paymentUrl()));
    }

    @PostMapping("/schedule-topup")
    public ResponseEntity<Map<String, String>> scheduleTopUp(
            org.springframework.security.core.Authentication authentication,
            @RequestBody(required = false) java.util.Map<String, Object> payload) {
        
        String cognitoSub = authentication.getName();
        System.out.println("Payload received: " + payload);
        if (payload == null) {
            throw new IllegalArgumentException(
                    "Payload is null! Body was empty.");
        }
        
        java.util.UUID studentId = java.util.UUID.fromString((String) payload.get("studentId"));
        java.math.BigDecimal amount = new java.math.BigDecimal(payload.get("amount").toString());
        Integer frequency = Integer.valueOf(payload.get("frequency").toString());
        String frequencyType = (String) payload.get("frequencyType");
        
        com.recred.backend.application.ScheduleWalletTopUpRequest request = 
                new com.recred.backend.application.ScheduleWalletTopUpRequest(
                        studentId, amount, frequency, frequencyType);

        com.recred.backend.application.ScheduleWalletTopUpResponse response = 
                scheduleWalletTopUp.execute(cognitoSub, request);

        return ResponseEntity.ok(Map.of("paymentUrl", response.preapprovalUrl()));
    }
}
