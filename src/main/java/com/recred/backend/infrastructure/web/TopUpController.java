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

    public TopUpController(GenerateTopUpLink generateTopUpLink) {
        this.generateTopUpLink = generateTopUpLink;
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
}
