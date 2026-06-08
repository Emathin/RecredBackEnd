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

        // 1. Intentamos leer desde el JSON body (Formato oficial de Webhooks v1)
        if (body != null) {
            type = (String) body.get("type");
            
            if (body.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null && data.get("id") != null) {
                    paymentId = String.valueOf(data.get("id"));
                }
            } else if (body.get("id") != null) {
                paymentId = String.valueOf(body.get("id"));
            }
        }

        // 2. Fallback: Intentamos leer desde los Query Parameters (Formato IPN o testeos manuales)
        if (paymentId == null) {
            paymentId = queryParams.get("data.id") != null 
                    ? queryParams.get("data.id") : queryParams.get("id");
        }
        if (type == null) {
            type = queryParams.get("type") != null 
                    ? queryParams.get("type") : queryParams.get("topic");
        }

        // Solo procesamos eventos de tipo 'payment'
        if ("payment".equals(type) && paymentId != null) {

            // 1. Validamos la autenticidad de la notificacion (si viene la firma)
            if (signature != null && 
                    !externalPaymentService.isValidSignature(signature, requestId, paymentId)) {
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
