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

    private final String PAYMENT_ID = "999";
    private final String SIGNATURE = "ts=123,v1=hash";
    private final String REQUEST_ID = "req-123";

    @Test
    void handleWebhookWhenSignatureIsValidAndPaymentIsTopUpShouldCreditWallet() {
        givenValidSignature();
        PaymentDetails details = givenValidTopUpPayment();
        
        ResponseEntity<Void> response = whenHandlingWebhookForPayment();
        
        thenReturnsOkAndCreditsWallet(response, details);
    }

    @Test
    void handleWebhookWhenPaymentIsUserSubscriptionShouldConfirmSubscription() {
        PaymentDetails details = givenSubscriptionPayment();

        ResponseEntity<Void> response = whenHandlingWebhookForPaymentWithoutHeaders();

        thenReturnsOkAndConfirmsSubscription(response, details);
    }

    @Test
    void handleWebhookWhenSignatureIsInvalidShouldReturnForbidden() {
        givenInvalidSignature();

        ResponseEntity<Void> response = whenHandlingWebhookForPayment();

        thenReturnsForbiddenAndDoesNothing(response);
    }

    @Test
    void handleWebhookWhenEventIsNotPaymentShouldIgnoreAndReturnOk() {
        ResponseEntity<Void> response = whenHandlingWebhookForNonPayment();

        thenReturnsOkAndDoesNothing(response);
    }

    @Test
    void handleWebhookWhenBodyIdExistsShouldProcessPayment() {
        PaymentDetails details = givenValidTopUpPayment();

        ResponseEntity<Void> response = whenHandlingWebhookWithIdInBody();

        thenReturnsOkAndCreditsWallet(response, details);
    }

    @Test
    void handleWebhookWhenQueryDataIdAndTopicExistShouldProcessPayment() {
        PaymentDetails details = givenValidTopUpPayment();

        ResponseEntity<Void> response = whenHandlingWebhookWithIdInParams();

        thenReturnsOkAndCreditsWallet(response, details);
    }

    @Test
    void handleWebhookWhenBodyIsNullAndSignatureInvalidShouldValidatePaymentIdOnly() {
        givenInvalidSignatureForParams();

        ResponseEntity<Void> response = whenHandlingWebhookWithParamsOnly();

        thenReturnsForbiddenAndValidatesSignatureOnly(response);
    }

    @Test
    void handleWebhookWhenBodyTypeIsNullStringShouldUseQueryTopic() {
        PaymentDetails details = givenValidTopUpPayment();

        ResponseEntity<Void> response = whenHandlingWebhookWithNullStringBodyType();

        thenReturnsOkAndCreditsWallet(response, details);
    }

    @Test
    void handleWebhookWhenApplicationFailsShouldReturnInternalServerError() {
        givenValidSignature();
        PaymentDetails details = givenValidTopUpPayment();
        givenDatabaseError(details);

        ResponseEntity<Void> response = whenHandlingWebhookForPayment();

        thenReturnsInternalServerError(response);
    }

    private PaymentDetails givenValidTopUpPayment() {
        PaymentDetails details = topUpPayment();
        when(externalPaymentService.getPaymentDetails(PAYMENT_ID)).thenReturn(details);
        return details;
    }

    private PaymentDetails givenSubscriptionPayment() {
        PaymentDetails details = subscriptionPayment();
        when(externalPaymentService.getPaymentDetails(PAYMENT_ID)).thenReturn(details);
        return details;
    }

    private void givenValidSignature() {
        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    private void givenInvalidSignature() {
        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString()))
                .thenReturn(false);
    }

    private void givenInvalidSignatureForParams() {
        when(externalPaymentService.isValidSignature(SIGNATURE, REQUEST_ID, PAYMENT_ID))
                .thenReturn(false);
    }

    private void givenDatabaseError(PaymentDetails details) {
        doThrow(new RuntimeException("DB Error")).when(confirmPaymentAndCredit)
                .execute(new ConfirmPaymentAndCreditRequest(PAYMENT_ID, details));
    }

    private ResponseEntity<Void> whenHandlingWebhookForPayment() {
        return paymentWebhookController.handleWebhook(SIGNATURE, REQUEST_ID, paymentBody(), new HashMap<>());
    }

    private ResponseEntity<Void> whenHandlingWebhookForPaymentWithoutHeaders() {
        return paymentWebhookController.handleWebhook(null, null, paymentBody(), new HashMap<>());
    }

    private ResponseEntity<Void> whenHandlingWebhookForNonPayment() {
        return paymentWebhookController.handleWebhook(null, null, nonPaymentBody(), new HashMap<>());
    }

    private ResponseEntity<Void> whenHandlingWebhookWithIdInBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        body.put("id", PAYMENT_ID);
        return paymentWebhookController.handleWebhook(null, null, body, new HashMap<>());
    }

    private ResponseEntity<Void> whenHandlingWebhookWithIdInParams() {
        Map<String, String> params = new HashMap<>();
        params.put("data.id", PAYMENT_ID);
        params.put("topic", "payment");
        return paymentWebhookController.handleWebhook(null, null, null, params);
    }

    private ResponseEntity<Void> whenHandlingWebhookWithParamsOnly() {
        Map<String, String> params = new HashMap<>();
        params.put("id", PAYMENT_ID);
        params.put("type", "payment");
        return paymentWebhookController.handleWebhook(SIGNATURE, REQUEST_ID, null, params);
    }

    private ResponseEntity<Void> whenHandlingWebhookWithNullStringBodyType() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "null");
        body.put("id", PAYMENT_ID);
        Map<String, String> params = new HashMap<>();
        params.put("topic", "payment");
        return paymentWebhookController.handleWebhook(null, null, body, params);
    }

    private void thenReturnsOkAndCreditsWallet(ResponseEntity<Void> response, PaymentDetails details) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(confirmPaymentAndCredit).execute(new ConfirmPaymentAndCreditRequest(PAYMENT_ID, details));
        verify(confirmUserSubscriptionPayment, never()).execute(any(ConfirmUserSubscriptionPaymentRequest.class));
    }

    private void thenReturnsOkAndConfirmsSubscription(ResponseEntity<Void> response, PaymentDetails details) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(confirmUserSubscriptionPayment).execute(new ConfirmUserSubscriptionPaymentRequest(details));
        verify(confirmPaymentAndCredit, never()).execute(any(ConfirmPaymentAndCreditRequest.class));
    }

    private void thenReturnsForbiddenAndDoesNothing(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(confirmPaymentAndCredit, never()).execute(any(ConfirmPaymentAndCreditRequest.class));
        verify(confirmUserSubscriptionPayment, never()).execute(any(ConfirmUserSubscriptionPaymentRequest.class));
    }

    private void thenReturnsOkAndDoesNothing(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(confirmPaymentAndCredit, never()).execute(any(ConfirmPaymentAndCreditRequest.class));
        verify(confirmUserSubscriptionPayment, never()).execute(any(ConfirmUserSubscriptionPaymentRequest.class));
    }

    private void thenReturnsForbiddenAndValidatesSignatureOnly(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(externalPaymentService).isValidSignature(SIGNATURE, REQUEST_ID, PAYMENT_ID);
        verify(confirmPaymentAndCredit, never()).execute(any(ConfirmPaymentAndCreditRequest.class));
    }

    private void thenReturnsInternalServerError(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    private Map<String, Object> paymentBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        Map<String, Object> data = new HashMap<>();
        data.put("id", PAYMENT_ID);
        body.put("data", data);
        return body;
    }

    private Map<String, Object> nonPaymentBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "merchant_order");
        return body;
    }

    private PaymentDetails topUpPayment() {
        return new PaymentDetails(
                PAYMENT_ID,
                UUID.randomUUID(),
                new BigDecimal("1500.00"),
                PaymentStatus.APPROVED
        );
    }

    private PaymentDetails subscriptionPayment() {
        return new PaymentDetails(
                PAYMENT_ID,
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
