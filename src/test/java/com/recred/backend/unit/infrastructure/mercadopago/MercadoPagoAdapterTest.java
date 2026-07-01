package com.recred.backend.unit.infrastructure.mercadopago;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.net.MPResponse;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.config.MercadoPagoProperties;
import com.recred.backend.infrastructure.mercadopago.MercadoPagoAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
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

    @Test
    void isValidSignature_WhenSignaturePartsAreMissingOrSecretFails_ShouldReturnFalse() {
        assertFalse(mercadoPagoAdapter.isValidSignature("ts=123", "req", "999"));
        assertFalse(mercadoPagoAdapter.isValidSignature("v1=abc", "req", "999"));
        when(properties.getWebhookSecret()).thenReturn(null);
        assertFalse(mercadoPagoAdapter.isValidSignature("ts=123,v1=abc", "req", "999"));
    }

    @Test
    void createTopUpPreference_ShouldCreatePreferenceAndReturnInitPoint()
            throws Exception {
        UUID studentId = UUID.randomUUID();
        Preference preference = org.mockito.Mockito.mock(Preference.class);
        when(preference.getInitPoint()).thenReturn("https://mp.test/init");
        when(properties.getSuccessUrl()).thenReturn(" https://front.test/success ");
        when(properties.getPendingUrl()).thenReturn(" https://front.test/pending ");
        when(properties.getFailureUrl()).thenReturn(" https://front.test/failure ");
        when(properties.getNotificationUrl()).thenReturn(" https://api.test/webhook ");

        try (MockedConstruction<PreferenceClient> mocked = mockConstruction(
                PreferenceClient.class,
                (client, context) -> when(client.create(any(PreferenceRequest.class)))
                        .thenReturn(preference)
        )) {
            String result = mercadoPagoAdapter.createTopUpPreference(
                    studentId,
                    new BigDecimal("1500.00")
            );

            assertEquals("https://mp.test/init", result);
            PreferenceClient client = mocked.constructed().get(0);
            ArgumentCaptor<PreferenceRequest> requestCaptor =
                    ArgumentCaptor.forClass(PreferenceRequest.class);
            verify(client).create(requestCaptor.capture());
            assertNotNull(requestCaptor.getValue());
        }
    }

    @Test
    void createTopUpPreference_WhenClientFails_ShouldWrapException() throws Exception {
        when(properties.getSuccessUrl()).thenReturn("https://front.test/success");
        when(properties.getPendingUrl()).thenReturn("https://front.test/pending");
        when(properties.getFailureUrl()).thenReturn("https://front.test/failure");
        when(properties.getNotificationUrl()).thenReturn("https://api.test/webhook");

        try (MockedConstruction<PreferenceClient> ignored = mockConstruction(
                PreferenceClient.class,
                (client, context) -> when(client.create(any(PreferenceRequest.class)))
                        .thenThrow(new RuntimeException("sdk error"))
        )) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> mercadoPagoAdapter.createTopUpPreference(
                            UUID.randomUUID(),
                            new BigDecimal("1500.00")
                    ));

            assertEquals("Error al crear preferencia de Mercado Pago",
                    exception.getMessage());
        }
    }

    @Test
    void createTopUpPreference_WhenApiFails_ShouldIncludeApiContent() throws Exception {
        when(properties.getSuccessUrl()).thenReturn("https://front.test/success");
        when(properties.getPendingUrl()).thenReturn("https://front.test/pending");
        when(properties.getFailureUrl()).thenReturn("https://front.test/failure");
        when(properties.getNotificationUrl()).thenReturn("https://api.test/webhook");
        MPApiException apiException = new MPApiException(
                "api error",
                new MPResponse(400, Map.of(), "invalid preference")
        );

        try (MockedConstruction<PreferenceClient> ignored = mockConstruction(
                PreferenceClient.class,
                (client, context) -> when(client.create(any(PreferenceRequest.class)))
                        .thenThrow(apiException)
        )) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> mercadoPagoAdapter.createTopUpPreference(
                            UUID.randomUUID(),
                            new BigDecimal("1500.00")
                    ));

            assertEquals("Error al crear preferencia: invalid preference",
                    exception.getMessage());
            assertEquals(apiException, exception.getCause());
        }
    }

    @Test
    void getPaymentDetails_ShouldMapApprovedPayment() throws Exception {
        UUID studentId = UUID.randomUUID();
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        when(payment.getExternalReference()).thenReturn(studentId.toString());
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getTransactionAmount()).thenReturn(new BigDecimal("1500.00"));

        try (MockedConstruction<PaymentClient> ignored = mockConstruction(
                PaymentClient.class,
                (client, context) -> when(client.get(123L)).thenReturn(payment)
        )) {
            PaymentDetails details = mercadoPagoAdapter.getPaymentDetails("123");

            assertEquals("123", details.paymentId());
            assertEquals(studentId, details.studentId());
            assertEquals(new BigDecimal("1500.00"), details.amount());
            assertEquals(PaymentStatus.APPROVED, details.status());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "pending,PENDING",
            "in_process,PENDING",
            "rejected,REJECTED",
            "refunded,REFUNDED",
            "charged_back,CHARGEBACK",
            "cancelled,CANCELLED",
            "unknown,PENDING"
    })
    void getPaymentDetails_ShouldMapMercadoPagoStatuses(
            String mercadoPagoStatus,
            PaymentStatus expectedStatus
    ) throws Exception {
        UUID studentId = UUID.randomUUID();
        Payment payment = org.mockito.Mockito.mock(Payment.class);
        when(payment.getExternalReference()).thenReturn(studentId.toString());
        when(payment.getStatus()).thenReturn(mercadoPagoStatus);
        when(payment.getTransactionAmount()).thenReturn(new BigDecimal("1500.00"));

        try (MockedConstruction<PaymentClient> ignored = mockConstruction(
                PaymentClient.class,
                (client, context) -> when(client.get(123L)).thenReturn(payment)
        )) {
            PaymentDetails details = mercadoPagoAdapter.getPaymentDetails("123");

            assertEquals(expectedStatus, details.status());
        }
    }

    @Test
    void getPaymentDetails_WhenClientFails_ShouldWrapException() throws Exception {
        try (MockedConstruction<PaymentClient> ignored = mockConstruction(
                PaymentClient.class,
                (client, context) -> when(client.get(123L))
                        .thenThrow(new RuntimeException("sdk error"))
        )) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> mercadoPagoAdapter.getPaymentDetails("123"));

            assertTrue(exception.getMessage().contains("123"));
        }
    }
}

