package com.recred.backend.unit.infrastructure.mercadopago;

import com.recred.backend.config.MercadoPagoProperties;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.infrastructure.mercadopago.MercadoPagoPayoutAdapter;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPayoutAdapterTest {

    @Mock
    private MercadoPagoProperties properties;

    @Mock
    private RestTemplate restTemplate;

    private MercadoPagoPayoutAdapter adapter;

    @BeforeEach
    void setUp() {
        when(properties.getAccessToken()).thenReturn("access-token");
        adapter = new MercadoPagoPayoutAdapter(properties);
        ReflectionTestUtils.setField(adapter, "restTemplate", restTemplate);
    }

    @ParameterizedTest
    @CsvSource({
            "approved,APPROVED,123",
            "rejected,REJECTED,123",
            "in_process,PENDING,123"
    })
    void transferToThirdPartyShouldMapPayoutStatus(
            String mercadoPagoStatus,
            PaymentStatus expectedStatus,
            String expectedPaymentId
    ) throws Exception {
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(payoutResponseClass())))
                .thenReturn(responseEntity(payoutResponse(123L, mercadoPagoStatus)));

        PaymentDetails details = adapter.transferToThirdParty(
                "0000003100000000000001",
                "20123456789",
                "Ana Gomez",
                new BigDecimal("1500.00"),
                "idem-123"
        );

        assertEquals(expectedPaymentId, details.paymentId());
        assertEquals(new BigDecimal("1500.00"), details.amount());
        assertEquals(expectedStatus, details.status());
    }

    @Test
    void transferToThirdPartyShouldUseIdempotencyKeyWhenPayoutIdIsNull()
            throws Exception {
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(payoutResponseClass())))
                .thenReturn(responseEntity(payoutResponse(null, "approved")));

        PaymentDetails details = adapter.transferToThirdParty(
                "0000003100000000000001",
                "20123456789",
                "Ana Gomez",
                new BigDecimal("1500.00"),
                "idem-123"
        );

        assertEquals("idem-123", details.paymentId());
        assertEquals(PaymentStatus.APPROVED, details.status());
    }

    @Test
    void transferToThirdPartyShouldReturnPendingWhenBodyIsNull() {
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(payoutResponseClass())))
                .thenReturn(ResponseEntity.ok(null));

        PaymentDetails details = adapter.transferToThirdParty(
                "0000003100000000000001",
                "20123456789",
                "Ana Gomez",
                new BigDecimal("1500.00"),
                "idem-123"
        );

        assertEquals("idem-123", details.paymentId());
        assertEquals(PaymentStatus.PENDING, details.status());
    }

    @Test
    void transferToThirdPartyShouldSendAuthorizationAndIdempotencyHeaders()
            throws Exception {
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(payoutResponseClass())))
                .thenReturn(responseEntity(payoutResponse(123L, "approved")));

        adapter.transferToThirdParty(
                "0000003100000000000001",
                "20123456789",
                "Ana Gomez",
                new BigDecimal("1500.00"),
                "idem-123"
        );

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.mercadopago.com/v1/payouts"),
                captor.capture(),
                eq(payoutResponseClass())
        );
        assertEquals("Bearer access-token",
                captor.getValue().getHeaders().getFirst("Authorization"));
        assertEquals("idem-123",
                captor.getValue().getHeaders().getFirst("X-Idempotency-Key"));
    }

    @Test
    void transferToThirdPartyShouldWrapClientErrors() {
        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(payoutResponseClass())))
                .thenThrow(new RuntimeException("mp down"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adapter.transferToThirdParty(
                        "0000003100000000000001",
                        "20123456789",
                        "Ana Gomez",
                        new BigDecimal("1500.00"),
                        "idem-123"
                ));

        assertTrue(exception.getMessage().startsWith("Error en la conexi"));
    }

    private ResponseEntity responseEntity(Object body) {
        return ResponseEntity.ok(body);
    }

    private Object payoutResponse(Long id, String status) throws Exception {
        Constructor<?> constructor = payoutResponseClass().getDeclaredConstructor(
                Long.class,
                String.class,
                String.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(id, status, "detail");
    }

    private Class<?> payoutResponseClass() {
        try {
            return Class.forName(
                    "com.recred.backend.infrastructure.mercadopago"
                            + ".MercadoPagoPayoutAdapter$PayoutResponse"
            );
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
