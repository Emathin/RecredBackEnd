package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TopUpControllerTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String TOP_UP_URL = "/api/v1/payments/topup";
    private static final String PAYMENT_LINK = "https://pay.test/link";

    @Mock
    private GenerateTopUpLink generateTopUpLink;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TopUpRequest topUpRequest;

    @BeforeEach
    void setUp() {
        TopUpController controller = new TopUpController(generateTopUpLink);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void requestTopUpLink_ShouldDelegateAndReturnPaymentUrl() throws Exception {
        givenTopUpRequest();
        givenPaymentLinkGenerated();

        ResultActions result = whenRequestingTopUpLink();

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

    private ResultActions whenRequestingTopUpLink() throws Exception {
        return mockMvc.perform(post(TOP_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topUpRequest)));
    }

    private void thenPaymentUrlIsReturned(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").value(PAYMENT_LINK));
    }

    private void thenDelegatesToGenerateTopUpLink() {
        ArgumentCaptor<GenerateTopUpLinkRequest> captor =
                ArgumentCaptor.forClass(GenerateTopUpLinkRequest.class);
        verify(generateTopUpLink).execute(captor.capture());

        assertEquals(STUDENT_ID, captor.getValue().studentId());
        assertEquals(AMOUNT, captor.getValue().amount());
    }
}
