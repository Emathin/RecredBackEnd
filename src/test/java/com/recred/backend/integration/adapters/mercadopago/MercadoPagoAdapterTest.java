package com.recred.backend.integration.adapters.mercadopago;

import com.recred.backend.config.MercadoPagoProperties;
import com.recred.backend.infrastructure.mercadopago.MercadoPagoAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoAdapterTest {

    @Mock
    private MercadoPagoProperties properties;

    private MercadoPagoAdapter mercadoPagoAdapter;

    @BeforeEach
    void setUp() {
        when(properties.getAccessToken()).thenReturn("test-token");
        mercadoPagoAdapter = new MercadoPagoAdapter(properties);
    }

    @Test
    void isValidSignature_ConDatosCorrectos_RetornaTrue() throws Exception {
        String secret = "test_secret";
        String ts = "1704908010";
        String requestId = "req-123";
        String paymentId = "999";
        
        String manifest = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";
        
        javax.crypto.spec.SecretKeySpec signingKey = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        StringBuilder sb = new StringBuilder(rawHmac.length * 2);
        java.util.Formatter formatter = new java.util.Formatter(sb);
        for (byte b : rawHmac) {
            formatter.format("%02x", b);
        }
        String validHash = sb.toString();
        
        String signature = "ts=" + ts + ",v1=" + validHash;

        when(properties.getWebhookSecret()).thenReturn(secret);

        boolean result = mercadoPagoAdapter.isValidSignature(signature, requestId, paymentId);

        assertTrue(result, "La firma deberia ser valida con el secreto y manifiesto correctos");
    }

    @Test
    void isValidSignature_ConHashIncorrecto_RetornaFalse() {
        String signature = "ts=123,v1=wrong_hash";
        when(properties.getWebhookSecret()).thenReturn("secret");

        boolean result = mercadoPagoAdapter.isValidSignature(signature, "req", "999");

        assertFalse(result);
    }

    @Test
    void isValidSignature_SinHeaders_RetornaFalse() {
        assertFalse(mercadoPagoAdapter.isValidSignature(null, "req", "999"));
        assertFalse(mercadoPagoAdapter.isValidSignature("sig", null, "999"));
        assertFalse(mercadoPagoAdapter.isValidSignature("sig", "req", null));
    }
}
