package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.application.ConfirmSchoolSubscriptionPayment;
import com.recred.backend.application.ConfirmUserSubscriptionPayment;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.config.SecurityConfig;
import com.recred.backend.domain.services.ExternalPaymentService;
import com.recred.backend.infrastructure.mercadopago.MercadoPagoWebhookParser;
import com.recred.backend.infrastructure.mercadopago.WebhookEvent;
import com.recred.backend.infrastructure.web.PaymentWebhookController;
import com.recred.backend.infrastructure.web.SchoolLicenseAccessGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentWebhookController.class)
@Import(SecurityConfig.class)
class PaymentWebhookSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmPaymentAndCredit confirmPaymentAndCredit;

    @MockitoBean
    private ConfirmUserSubscriptionPayment confirmUserSubscriptionPayment;

    @MockitoBean
    private ConfirmSchoolSubscriptionPayment confirmSchoolSubscriptionPayment;

    @MockitoBean
    private ExternalPaymentService externalPaymentService;

    @MockitoBean
    private MercadoPagoWebhookParser webhookParser;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    @Test
    void handleWebhook_WhenCalledWithoutAuthentication_ShouldReturnOk() throws Exception {
        givenNonPaymentWebhookEvent();

        ResultActions result = whenCallingWebhookWithoutAuthentication();

        result.andExpect(status().isOk());
    }

    private void givenNonPaymentWebhookEvent() {
        when(webhookParser.parseEvent(any(), any()))
                .thenReturn(new WebhookEvent(null, false, null));
    }

    private ResultActions whenCallingWebhookWithoutAuthentication() throws Exception {
        return mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }
}
