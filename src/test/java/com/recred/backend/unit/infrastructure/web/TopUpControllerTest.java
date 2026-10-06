package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GenerateTopUpLinkRequest;
import com.recred.backend.application.GenerateTopUpLinkResponse;
import com.recred.backend.infrastructure.web.TopUpController;
import com.recred.backend.infrastructure.web.TopUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopUpControllerTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String TOP_UP_URL = "/api/v1/payments/topup";
    private static final String PAYMENT_LINK = "https://pay.test/link";

    @Mock
    private GenerateTopUpLink generateTopUpLink;

    @Mock
    private com.recred.backend.application.ScheduleWalletTopUp scheduleWalletTopUp;

    private TopUpController controller;
    private TopUpRequest topUpRequest;

    @BeforeEach
    void setUp() {
        controller = new TopUpController(generateTopUpLink, scheduleWalletTopUp);
    }

    @Test
    void givenTopUpRequestWhenRequestingLinkThenDelegatesAndReturnsPaymentUrl() {
        givenTopUpRequest();
        givenPaymentLinkGenerated();

        ResponseEntity<Map<String, String>> result = whenRequestingTopUpLink();

        thenPaymentUrlIsReturned(result);
        thenDelegatesToGenerateTopUpLink();
    }

    private void givenTopUpRequest() {
        topUpRequest = new TopUpRequest(STUDENT_ID, AMOUNT);
    }

    private void givenPaymentLinkGenerated() {
        when(generateTopUpLink.execute(any(GenerateTopUpLinkRequest.class)))
                .thenReturn(new GenerateTopUpLinkResponse(PAYMENT_LINK));
    }

    private ResponseEntity<Map<String, String>> whenRequestingTopUpLink() {
        return controller.requestTopUpLink(topUpRequest);
    }

    private void thenPaymentUrlIsReturned(ResponseEntity<Map<String, String>> result) {
        assertEquals(200, result.getStatusCode().value());
        assertEquals(PAYMENT_LINK, result.getBody().get("paymentUrl"));
    }

    private void thenDelegatesToGenerateTopUpLink() {
        ArgumentCaptor<GenerateTopUpLinkRequest> captor =
                ArgumentCaptor.forClass(GenerateTopUpLinkRequest.class);
        verify(generateTopUpLink).execute(captor.capture());

        assertEquals(STUDENT_ID, captor.getValue().studentId());
        assertEquals(AMOUNT, captor.getValue().amount());
    }

    @Test
    void scheduleTopUp_returnsPreapprovalUrl() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("sub-123");

        Map<String, Object> payload = Map.of(
                "studentId", STUDENT_ID.toString(),
                "amount", AMOUNT.doubleValue(),
                "frequency", 1,
                "frequencyType", "months"
        );

        when(scheduleWalletTopUp.execute(eq("sub-123"), any(com.recred.backend.application.ScheduleWalletTopUpRequest.class)))
                .thenReturn(new com.recred.backend.application.ScheduleWalletTopUpResponse(PAYMENT_LINK));

        ResponseEntity<Map<String, String>> result = controller.scheduleTopUp(auth, payload);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(PAYMENT_LINK, result.getBody().get("paymentUrl"));

        ArgumentCaptor<com.recred.backend.application.ScheduleWalletTopUpRequest> captor = 
                ArgumentCaptor.forClass(com.recred.backend.application.ScheduleWalletTopUpRequest.class);
        verify(scheduleWalletTopUp).execute(eq("sub-123"), captor.capture());
        
        com.recred.backend.application.ScheduleWalletTopUpRequest capturedReq = captor.getValue();
        assertEquals(STUDENT_ID, capturedReq.studentId());
        assertEquals(1, capturedReq.frequency());
        assertEquals("months", capturedReq.frequencyType());
    }

    @Test
    void scheduleTopUp_nullPayload_throwsException() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("sub-123");

        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, 
                () -> controller.scheduleTopUp(auth, null)
        );
        assertEquals("Payload is null! Body was empty.", ex.getMessage());
    }
}
