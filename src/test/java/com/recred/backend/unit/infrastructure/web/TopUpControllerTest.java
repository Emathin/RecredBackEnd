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

    private TopUpController controller;
    private TopUpRequest topUpRequest;

    @BeforeEach
    void setUp() {
        controller = new TopUpController(generateTopUpLink);
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
}
