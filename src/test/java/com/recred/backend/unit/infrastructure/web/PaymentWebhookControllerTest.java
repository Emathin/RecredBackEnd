package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.application.ConfirmPaymentAndCreditRequest;
import com.recred.backend.application.ConfirmUserSubscriptionPayment;
import com.recred.backend.application.ConfirmUserSubscriptionPaymentRequest;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentPurpose;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.services.ExternalPaymentService;
import com.recred.backend.infrastructure.mercadopago.MercadoPagoWebhookParser;
import com.recred.backend.infrastructure.web.PaymentWebhookController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @Mock
    private ConfirmPaymentAndCredit confirmPaymentAndCredit;

    @Mock
    private ConfirmUserSubscriptionPayment confirmUserSubscriptionPayment;

    @Mock
    private ExternalPaymentService externalPaymentService;

    @Spy
    private MercadoPagoWebhookParser webhookParser;

    @InjectMocks
    private PaymentWebhookController paymentWebhookController;

    @Test
    void handleWebhook_WhenSignatureIsValidAndPaymentIsTopUp_ShouldCreditWallet() {
        String paymentId = "999";
        PaymentDetails details = topUpPayment(paymentId);
        givenValidSignature();
        givenPaymentDetails(paymentId, details);

        ResponseEntity<Void> response = whenHandlingWebhook(
                "ts=123,v1=hash",
                "req-123",
                paymentBody(paymentId),
                new HashMap<>()
        );

        thenReturnsOk(response);
        thenCreditsWallet(paymentId, details);
        thenDoesNotConfirmUserSubscription();
    }

    @Test
    void handleWebhook_WhenPaymentIsUserSubscription_ShouldConfirmSubscription() {
        String paymentId = "999";
        PaymentDetails details = subscriptionPayment(paymentId);
        givenPaymentDetails(paymentId, details);

        ResponseEntity<Void> response = whenHandlingWebhook(
                null,
                null,
                paymentBody(paymentId),
                new HashMap<>()
        );

        thenReturnsOk(response);
        thenConfirmsUserSubscription(details);
        thenDoesNotCreditWallet();
    }

    @Test
    void handleWebhook_WhenSignatureIsInvalid_ShouldReturnForbidden() {
        String paymentId = "999";
        givenInvalidSignature();

        ResponseEntity<Void> response = whenHandlingWebhook(
                "ts=123,v1=hash",
                "req-123",
                paymentBody(paymentId),
                new HashMap<>()
        );

        thenReturnsForbidden(response);
        thenDoesNotCreditWallet();
        thenDoesNotConfirmUserSubscription();
    }

    @Test
    void handleWebhook_WhenEventIsNotPayment_ShouldIgnoreAndReturnOk() {
        ResponseEntity<Void> response = whenHandlingWebhook(
                null,
                null,
                nonPaymentBody(),
                new HashMap<>()
        );

        thenReturnsOk(response);
        thenDoesNotCreditWallet();
        thenDoesNotConfirmUserSubscription();
    }

    @Test
    void handleWebhook_WhenBodyIdExists_ShouldProcessPayment() {
        String paymentId = "888";
        PaymentDetails details = topUpPayment(paymentId);
        givenPaymentDetails(paymentId, details);
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        body.put("id", paymentId);

        ResponseEntity<Void> response = whenHandlingWebhook(null, null, body, new HashMap<>());

        thenReturnsOk(response);
        thenCreditsWallet(paymentId, details);
    }

    @Test
    void handleWebhook_WhenQueryDataIdAndTopicExist_ShouldProcessPayment() {
        String paymentId = "777";
        PaymentDetails details = topUpPayment(paymentId);
        givenPaymentDetails(paymentId, details);
        Map<String, String> params = new HashMap<>();
        params.put("data.id", paymentId);
        params.put("topic", "payment");

        ResponseEntity<Void> response = whenHandlingWebhook(null, null, null, params);

        thenReturnsOk(response);
        thenCreditsWallet(paymentId, details);
    }

    @Test
    void handleWebhook_WhenBodyIsNullAndSignatureInvalid_ShouldValidatePaymentIdOnly() {
        Map<String, String> params = new HashMap<>();
        params.put("id", "666");
        params.put("type", "payment");
        when(externalPaymentService.isValidSignature("sig", "req", "666"))
                .thenReturn(false);

        ResponseEntity<Void> response = whenHandlingWebhook("sig", "req", null, params);

        thenReturnsForbidden(response);
        thenValidatesPaymentIdSignatureOnly("sig", "req", "666");
        thenDoesNotCreditWallet();
    }

    @Test
    void handleWebhook_WhenBodyTypeIsNullString_ShouldUseQueryTopic() {
        String paymentId = "555";
        PaymentDetails details = topUpPayment(paymentId);
        givenPaymentDetails(paymentId, details);
        Map<String, Object> body = new HashMap<>();
        body.put("type", "null");
        body.put("id", paymentId);
        Map<String, String> params = new HashMap<>();
        params.put("topic", "payment");

        ResponseEntity<Void> response = whenHandlingWebhook(null, null, body, params);

        thenReturnsOk(response);
        thenCreditsWallet(paymentId, details);
    }

    @Test
    void handleWebhook_WhenApplicationFails_ShouldReturnInternalServerError() {
        String paymentId = "999";
        PaymentDetails details = topUpPayment(paymentId);
        givenValidSignature();
        givenPaymentDetails(paymentId, details);
        doThrow(new RuntimeException("DB Error")).when(confirmPaymentAndCredit)
                .execute(new ConfirmPaymentAndCreditRequest(paymentId, details));

        ResponseEntity<Void> response = whenHandlingWebhook(
                "ts=123,v1=hash",
                "req-id",
                paymentBody(paymentId),
                new HashMap<>()
        );

        thenReturnsInternalServerError(response);
    }

    private void givenValidSignature() {
        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    private void givenInvalidSignature() {
        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString()))
                .thenReturn(false);
    }

    private void givenPaymentDetails(String paymentId, PaymentDetails details) {
        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);
    }

    private ResponseEntity<Void> whenHandlingWebhook(
            String signature,
            String requestId,
            Map<String, Object> body,
            Map<String, String> params
    ) {
        return paymentWebhookController.handleWebhook(signature, requestId, body, params);
    }

    private void thenReturnsOk(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void thenReturnsForbidden(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private void thenReturnsInternalServerError(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    private void thenCreditsWallet(String paymentId, PaymentDetails details) {
        verify(confirmPaymentAndCredit)
                .execute(new ConfirmPaymentAndCreditRequest(paymentId, details));
    }

    private void thenConfirmsUserSubscription(PaymentDetails details) {
        verify(confirmUserSubscriptionPayment)
                .execute(new ConfirmUserSubscriptionPaymentRequest(details));
    }

    private void thenDoesNotCreditWallet() {
        verify(confirmPaymentAndCredit, never()).execute(any(ConfirmPaymentAndCreditRequest.class));
    }

    private void thenDoesNotConfirmUserSubscription() {
        verify(confirmUserSubscriptionPayment, never()).execute(
                any(ConfirmUserSubscriptionPaymentRequest.class));
    }

    private void thenValidatesPaymentIdSignatureOnly(String sig, String req, String id) {
        verify(externalPaymentService).isValidSignature(sig, req, id);
    }

    private Map<String, Object> paymentBody(String paymentId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        Map<String, Object> data = new HashMap<>();
        data.put("id", paymentId);
        body.put("data", data);
        return body;
    }

    private Map<String, Object> nonPaymentBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "merchant_order");
        return body;
    }

    private PaymentDetails topUpPayment(String paymentId) {
        return new PaymentDetails(
                paymentId,
                UUID.randomUUID(),
                new BigDecimal("1500.00"),
                PaymentStatus.APPROVED
        );
    }

    private PaymentDetails subscriptionPayment(String paymentId) {
        return new PaymentDetails(
                paymentId,
                null,
                new BigDecimal("5500.00"),
                PaymentStatus.APPROVED,
                PaymentPurpose.USER_SUBSCRIPTION,
                UUID.randomUUID(),
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );
    }
}
