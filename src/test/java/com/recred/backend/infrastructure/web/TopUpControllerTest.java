package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.TopUpCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopUpControllerTest {

    @Test
    void requestTopUpLink_ShouldDelegateAndReturnPaymentUrl() {
        GenerateTopUpLink generateTopUpLink = mock(GenerateTopUpLink.class);
        TopUpController controller = new TopUpController(generateTopUpLink);
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(generateTopUpLink.execute(new TopUpCommand(studentId, amount)))
                .thenReturn("https://pay.test/link");

        ResponseEntity<Map<String, String>> response = controller.requestTopUpLink(
                new TopUpRequest(studentId, amount)
        );

        ArgumentCaptor<TopUpCommand> captor = ArgumentCaptor.forClass(TopUpCommand.class);
        verify(generateTopUpLink).execute(captor.capture());
        assertEquals(studentId, captor.getValue().studentId());
        assertEquals(amount, captor.getValue().amount());
        assertEquals("https://pay.test/link", response.getBody().get("paymentUrl"));
    }
}
