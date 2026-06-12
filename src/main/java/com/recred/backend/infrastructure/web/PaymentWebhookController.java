package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController {

    private final ConfirmPaymentAndCredit confirmPaymentAndCredit;
    private final ExternalPaymentService externalPaymentService;

    public PaymentWebhookController(
            ConfirmPaymentAndCredit confirmPaymentAndCredit,
            ExternalPaymentService externalPaymentService) {
        this.confirmPaymentAndCredit = confirmPaymentAndCredit;
        this.externalPaymentService = externalPaymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam Map<String, String> queryParams) {

        String paymentId = null;
        String type = null;

        if (body != null) {
            type = (String) body.get("type");

            if (body.containsKey("data")) {
                Map<?, ?> data = (Map<?, ?>) body.get("data");
                if (data != null && data.get("id") != null) {
                    paymentId = String.valueOf(data.get("id"));
                }
            } else if (body.get("id") != null) {
                paymentId = String.valueOf(body.get("id"));
            }
        }

        if (paymentId == null || "null".equals(paymentId)) {
            paymentId = queryParams.get("data.id") != null
                    ? queryParams.get("data.id") : queryParams.get("id");
        }
        if (type == null || "null".equals(type)) {
            type = queryParams.get("type") != null
                    ? queryParams.get("type") : queryParams.get("topic");
        }

        if ("payment".equals(type) && paymentId != null
                && !paymentId.isEmpty() && !"null".equals(paymentId)) {

            if (signature != null) {
                String notificationId = body != null ? String.valueOf(body.get("id")) : null;
                if (!externalPaymentService.isValidSignature(signature, requestId, paymentId)) {
                    if (notificationId != null && !externalPaymentService.isValidSignature(
                            signature, requestId, notificationId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }

            try {
                confirmPaymentAndCredit.execute(paymentId);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.ok().build();
    }
}
