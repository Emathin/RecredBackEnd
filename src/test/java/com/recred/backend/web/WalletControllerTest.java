package com.recred.backend.web;

import com.recred.backend.application.GetStudentWalletSummary;
import com.recred.backend.application.GetStudentWalletSummaryRequest;
import com.recred.backend.application.GetStudentWalletSummaryResponse;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoadBalanceToChild loadBalanceToChild;

    @Mock
    private TransferBalanceBetweenChildren transferBalanceBetweenChildren;

    @Mock
    private GetStudentWalletSummary getStudentWalletSummary;

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
        UUID studentId = randomUUID();
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

        verify(loadBalanceToChild).execute(new com.recred.backend.application.LoadBalanceToChildRequest("cognito-sub", studentId, amount));
    }

    @Test
    void transferBalanceReturns200AndDelegatesToUseCase() throws Exception {
        UUID fromId = randomUUID();
        UUID toId = randomUUID();
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

        verify(transferBalanceBetweenChildren).execute(new com.recred.backend.application.TransferBalanceBetweenChildrenRequest("cognito-sub", fromId, toId, amount));
    }

    @Test
    void getStudentWalletSummaryReturns200AndDelegatesToUseCase() throws Exception {
        UUID studentId = randomUUID();
        GetStudentWalletSummaryRequest request = new GetStudentWalletSummaryRequest(
                "cognito-sub",
                studentId,
                java.util.Optional.of(LocalDate.of(2026, 6, 1)),
                java.util.Optional.of(LocalDate.of(2026, 6, 14))
        );
        when(getStudentWalletSummary.execute(request)).thenReturn(new GetStudentWalletSummaryResponse(
                studentId,
                new BigDecimal("1250.00"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 14),
                new BigDecimal("3000.00"),
                new BigDecimal("1750.00"),
                new BigDecimal("1250.00"),
                8,
                List.of(),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/v1/wallets/students/{studentId}/summary", studentId)
                        .principal(authentication())
                        .param("desde", "2026-06-01")
                        .param("hasta", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoId").value(studentId.toString()))
                .andExpect(jsonPath("$.saldoActual").value(1250.00))
                .andExpect(jsonPath("$.periodo.desde").value("2026-06-01"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-06-14"))
                .andExpect(jsonPath("$.cantidadCompras").value(8));

        verify(getStudentWalletSummary).execute(request);
    }

    @Test
    void loadBalanceWithoutAuthenticationShouldThrow() {
        WalletController.LoadBalanceRequest request = new WalletController.LoadBalanceRequest(
                randomUUID(),
                BigDecimal.TEN
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> walletController.loadBalance(request, null));
    }

    @Test
    void transferBalanceWithInvalidAuthenticationPrincipalShouldThrow() {
        WalletController.TransferBalanceRequest request =
                new WalletController.TransferBalanceRequest(
                        randomUUID(),
                        randomUUID(),
                        BigDecimal.TEN
                );
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("subject", null);

        assertThrows(SecurityException.class,
                () -> walletController.transferBalance(request, authentication));
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
