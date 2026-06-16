package com.recred.backend.web;

import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import com.recred.backend.infrastructure.web.WalletController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoadBalanceToChild loadBalanceToChild;

    @Mock
    private TransferBalanceBetweenChildren transferBalanceBetweenChildren;

    @InjectMocks
    private WalletController walletController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(walletController)
                .build();
    }

    @Test
    void loadBalanceReturns200AndDelegatesToUseCase() throws Exception {
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        String requestBody = String.format(
                "{\"studentId\":\"%s\",\"amount\":%s}", 
                studentId, amount
        );

        mockMvc.perform(post("/api/v1/wallets/load")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(loadBalanceToChild).execute("cognito-sub", studentId, amount);
    }

    @Test
    void transferBalanceReturns200AndDelegatesToUseCase() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        String requestBody = String.format(
                "{\"fromStudentId\":\"%s\",\"toStudentId\":\"%s\",\"amount\":%s}",
                fromId, toId, amount
        );

        mockMvc.perform(post("/api/v1/wallets/transfer")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(transferBalanceBetweenChildren).execute("cognito-sub", fromId, toId, amount);
    }

    private Authentication authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
