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
            @RequestParam Map<String, String> params) {

        // El ID del pago llega en 'data.id' segun documentacion
        String paymentId = params.get("data.id") != null 
                ? params.get("data.id") : params.get("id");
        String type = params.get("type");

        // Solo procesamos eventos de tipo 'payment'
        if ("payment".equals(type) && paymentId != null) {

            // 1. Validamos la autenticidad de la notificacion
            if (!externalPaymentService.isValidSignature(signature, requestId, paymentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 2. Procesamos el pago de forma asincrona (transactional)
            try {
                confirmPaymentAndCredit.execute(paymentId);
            } catch (Exception e) {
                // En caso de error, devolvemos error para que MP reintente
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        // Devolvemos 200 OK inmediatamente (menos de 22 seg) para confirmar recepcion
        return ResponseEntity.ok().build();
    }
}
