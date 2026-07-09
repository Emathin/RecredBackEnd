package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.application.ConfirmPaymentAndCreditRequest;

import com.recred.backend.application.ConfirmUserSubscriptionPayment;
import com.recred.backend.application.ConfirmUserSubscriptionPaymentRequest;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.services.ExternalPaymentService;
import com.recred.backend.infrastructure.mercadopago.MercadoPagoWebhookParser;
import com.recred.backend.infrastructure.mercadopago.WebhookEvent;
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
    private final ConfirmUserSubscriptionPayment confirmUserSubscriptionPayment;

    private final ExternalPaymentService externalPaymentService;
    private final MercadoPagoWebhookParser webhookParser;

    public PaymentWebhookController(
            ConfirmPaymentAndCredit confirmPaymentAndCredit,
            ConfirmUserSubscriptionPayment confirmUserSubscriptionPayment,
            ExternalPaymentService externalPaymentService,
            MercadoPagoWebhookParser webhookParser) {
        this.confirmPaymentAndCredit = confirmPaymentAndCredit;
        this.confirmUserSubscriptionPayment = confirmUserSubscriptionPayment;
        this.externalPaymentService = externalPaymentService;
        this.webhookParser = webhookParser;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Map<String, String> queryParams) {
        
        if (queryParams == null) {
            queryParams = Map.of();
        }

        WebhookEvent event = webhookParser.parseEvent(body, queryParams);

        if (!event.isPaymentEvent()) {
            return ResponseEntity.ok().build();
        }

        if (signature != null && !isSignatureValid(signature, requestId, event)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            processPayment(event.paymentId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void processPayment(String paymentId) {
        PaymentDetails details = externalPaymentService.getPaymentDetails(paymentId);
        if (details.isUserSubscription()) {
            confirmUserSubscriptionPayment.execute(
                    new ConfirmUserSubscriptionPaymentRequest(details));
            return;
        }


        confirmPaymentAndCredit.execute(new ConfirmPaymentAndCreditRequest(paymentId, details));
    }

    private boolean isSignatureValid(String signature, String requestId, WebhookEvent event) {
        if (externalPaymentService.isValidSignature(signature, requestId, event.paymentId())) {
            return true;
        }
        return event.notificationId() != null && 
               externalPaymentService.isValidSignature(
                       signature, requestId, event.notificationId());
    }
}
