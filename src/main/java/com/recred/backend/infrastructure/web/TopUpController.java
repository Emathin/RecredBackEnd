package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GenerateTopUpLinkRequest;
import com.recred.backend.application.GenerateTopUpLinkResponse;
import com.recred.backend.application.ScheduleWalletTopUp;
import com.recred.backend.application.ScheduleWalletTopUpRequest;
import com.recred.backend.application.ScheduleWalletTopUpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class TopUpController {

    private final GenerateTopUpLink generateTopUpLink;
    private final ScheduleWalletTopUp scheduleWalletTopUp;

    public TopUpController(
            GenerateTopUpLink generateTopUpLink,
            ScheduleWalletTopUp scheduleWalletTopUp
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
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> payload) {

        String cognitoSub = authentication.getName();
        if (payload == null) {
            throw new IllegalArgumentException("Payload is null! Body was empty.");
        }

        UUID studentId = UUID.fromString((String) payload.get("studentId"));
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        Integer frequency = Integer.valueOf(payload.get("frequency").toString());
        String frequencyType = (String) payload.get("frequencyType");

        OffsetDateTime startDate = null;
        Object startDateObj = payload.get("startDate");
        if (startDateObj != null && !startDateObj.toString().isBlank()) {
            startDate = OffsetDateTime.parse(startDateObj.toString());
        }

        ScheduleWalletTopUpRequest request = new ScheduleWalletTopUpRequest(
                studentId, amount, frequency, frequencyType, startDate);

        ScheduleWalletTopUpResponse response = scheduleWalletTopUp.execute(cognitoSub, request);

        return ResponseEntity.ok(Map.of("paymentUrl", response.preapprovalUrl()));
    }
}
