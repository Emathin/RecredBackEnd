package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @Mock
    private ConfirmPaymentAndCredit confirmPaymentAndCredit;

    @Mock
    private ExternalPaymentService externalPaymentService;

    @InjectMocks
    private PaymentWebhookController paymentWebhookController;

    @Test
    void handleWebhook_ConFirmaValida_ProcesaPagoYDevuelveOk() {
        String signature = "ts=123,v1=hash";
        String requestId = "req-123";
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        Map<String, Object> data = new HashMap<>();
        data.put("id", "999");
        body.put("data", data);
        Map<String, String> params = new HashMap<>();

        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString())).thenReturn(true);

        ResponseEntity<Void> response = paymentWebhookController.handleWebhook(signature, requestId, body, params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(confirmPaymentAndCredit).execute("999");
    }

    @Test
    void handleWebhook_ConFirmaInvalida_DevuelveForbidden() {
        String signature = "ts=123,v1=hash";
        String requestId = "req-123";
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        Map<String, Object> data = new HashMap<>();
        data.put("id", "999");
        body.put("data", data);
        Map<String, String> params = new HashMap<>();

        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString())).thenReturn(false);

        ResponseEntity<Void> response = paymentWebhookController.handleWebhook(signature, requestId, body, params);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(confirmPaymentAndCredit, never()).execute(anyString());
    }

    @Test
    void handleWebhook_NotificacionNoEsDePago_IgnoraYDevuelveOk() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "merchant_order");
        Map<String, String> params = new HashMap<>();

        ResponseEntity<Void> response = paymentWebhookController.handleWebhook(null, null, body, params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(confirmPaymentAndCredit, never()).execute(anyString());
    }

    @Test
    void handleWebhook_ErrorEnAplicacion_DevuelveInternalError() {
        String signature = "ts=123,v1=hash";
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        Map<String, Object> data = new HashMap<>();
        data.put("id", "999");
        body.put("data", data);
        Map<String, String> params = new HashMap<>();

        when(externalPaymentService.isValidSignature(anyString(), anyString(), anyString())).thenReturn(true);
        doThrow(new RuntimeException("DB Error")).when(confirmPaymentAndCredit).execute("999");

        ResponseEntity<Void> response = paymentWebhookController.handleWebhook(signature, "req-id", body, params);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
