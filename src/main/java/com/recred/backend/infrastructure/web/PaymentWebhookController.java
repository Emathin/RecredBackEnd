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

        String type = extractType(body, queryParams);
        String paymentId = extractPaymentId(body, queryParams);

        if (!isPaymentEvent(type, paymentId)) {
            return ResponseEntity.ok().build();
        }

        if (signature != null && !isSignatureValid(signature, requestId,
                paymentId, body)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            confirmPaymentAndCredit.execute(paymentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String extractType(Map<String, Object> body,
                               Map<String, String> queryParams) {
        String type = body != null && body.get("type") != null ?
                String.valueOf(body.get("type")) : null;
        if (type == null || "null".equals(type)) {
            type = queryParams.get("type") != null ?
                    queryParams.get("type") : queryParams.get("topic");
        }
        return type;
    }

    private String extractPaymentId(Map<String, Object> body,
                                    Map<String, String> queryParams) {
        String paymentId = null;
        if (body != null) {
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
            paymentId = queryParams.get("data.id") != null ? queryParams.get("data.id") :
                    queryParams.get("id");
        }
        return paymentId;
    }

    private boolean isPaymentEvent(String type, String paymentId) {
        return "payment".equals(type) && paymentId != null && !paymentId.isEmpty() &&
                !"null".equals(paymentId);
    }

    private boolean isSignatureValid(String signature, String requestId,
                                     String paymentId, Map<String, Object> body) {
        if (externalPaymentService.isValidSignature(signature, requestId, paymentId)) {
            return true;
        }
        
        String notificationId = body != null && body.get("id") != null ?
                String.valueOf(body.get("id")) : null;
        if (notificationId != null && !"null".equals(notificationId)) {
            return externalPaymentService.isValidSignature(signature, requestId, notificationId);
        }
        
        return false;
    }
}
