package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummary;
import com.recred.backend.application.GetStudentWalletSummaryRequest;
import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.LoadBalanceToChildRequest;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import com.recred.backend.application.TransferBalanceBetweenChildrenRequest;
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.WalletController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest {

    private static final String COGNITO_SUBJECT = "cognito-sub";

    private final LoadBalanceToChild loadBalanceToChild = mock(LoadBalanceToChild.class);
    private final TransferBalanceBetweenChildren transferBalanceBetweenChildren =
            mock(TransferBalanceBetweenChildren.class);
    private final GetStudentWalletSummary getStudentWalletSummary =
            mock(GetStudentWalletSummary.class);
    private final PlanAccessGuard planAccessGuard = mock(PlanAccessGuard.class);
    private final WalletController walletController = new WalletController(
            loadBalanceToChild,
            transferBalanceBetweenChildren,
            getStudentWalletSummary,
            planAccessGuard
    );

    private MockMvc mockMvc;
    private UUID studentId;
    private UUID fromStudentId;
    private UUID toStudentId;
    private BigDecimal amount;
    private ResultActions result;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loadBalanceReturns200AndDelegatesToUseCase() throws Exception {
        givenLoadBalanceRequest();

        whenLoadingBalance();

        thenResponseIsOk();
        thenLoadBalanceWasDelegated();
    }

    @Test
    void transferBalanceReturns200AndDelegatesToUseCase() throws Exception {
        givenTransferBalanceRequest();

        whenTransferringBalance();

        thenResponseIsOk();
        thenTransferBalanceWasDelegated();
    }

    @Test
    void transferBalanceWithoutSourceStudentReturnsBadRequest() throws Exception {
        givenTransferBalanceRequestWithoutSourceStudent();

        whenPostingTransferBalance();

        thenResponseIsBadRequest();
        thenTransferBalanceWasNotDelegated();
    }

    @Test
    void transferBalanceWithoutDestinationStudentReturnsBadRequest() throws Exception {
        givenTransferBalanceRequestWithoutDestinationStudent();

        whenPostingTransferBalance();

        thenResponseIsBadRequest();
        thenTransferBalanceWasNotDelegated();
    }

    @Test
    void transferBalanceWithoutAmountReturnsBadRequest() throws Exception {
        givenTransferBalanceRequestWithoutAmount();

        whenPostingTransferBalance();

        thenResponseIsBadRequest();
        thenTransferBalanceWasNotDelegated();
    }

    @Test
    void transferBalanceWithNonPositiveAmountReturnsBadRequest() throws Exception {
        givenTransferBalanceRequestWithZeroAmount();

        whenPostingTransferBalance();

        thenResponseIsBadRequest();
        thenTransferBalanceWasNotDelegated();
    }

    @Test
    void transferBalanceToSameStudentReturnsBadRequest() throws Exception {
        givenTransferBalanceRequestToSameStudent();

        whenPostingTransferBalance();

        thenResponseIsBadRequest();
        thenTransferBalanceWasNotDelegated();
    }

    @Test
    void getStudentWalletSummaryReturns200AndDelegatesToUseCase() throws Exception {
        givenWalletSummary();

        whenGettingWalletSummary();

        thenWalletSummaryIsReturned();
        thenWalletSummaryWasDelegated();
    }

    @Test
    void loadBalanceWithoutAuthenticationShouldThrow() {
        givenLoadBalanceRequest();

        whenLoadingBalanceWithoutAuthenticationFails();
    }

    @Test
    void transferBalanceWithInvalidAuthenticationPrincipalShouldThrow() {
        givenTransferControllerRequest();

        whenTransferBalanceWithInvalidAuthenticationFails();
    }

    private void givenLoadBalanceRequest() {
        studentId = randomUUID();
        amount = new BigDecimal("100.00");
    }

    private void givenTransferBalanceRequest() {
        fromStudentId = randomUUID();
        toStudentId = randomUUID();
        amount = new BigDecimal("50.00");
    }

    private void givenTransferBalanceRequestWithoutSourceStudent() {
        givenTransferBalanceRequest();
        fromStudentId = null;
    }

    private void givenTransferBalanceRequestWithoutDestinationStudent() {
        givenTransferBalanceRequest();
        toStudentId = null;
    }

    private void givenTransferBalanceRequestWithoutAmount() {
        givenTransferBalanceRequest();
        amount = null;
    }

    private void givenTransferBalanceRequestWithZeroAmount() {
        givenTransferBalanceRequest();
        amount = BigDecimal.ZERO;
    }

    private void givenTransferBalanceRequestToSameStudent() {
        givenTransferBalanceRequest();
        toStudentId = fromStudentId;
    }

    private void givenTransferControllerRequest() {
        givenTransferBalanceRequest();
    }

    private void givenWalletSummary() {
        studentId = randomUUID();
        when(getStudentWalletSummary.execute(summaryRequest()))
                .thenReturn(summaryResponse());
    }

    private void whenLoadingBalance() throws Exception {
        result = postJson("/api/v1/wallets/load", loadBalanceBody());
    }

    private void whenTransferringBalance() throws Exception {
        result = postJson("/api/v1/wallets/transfer", transferBalanceBody());
    }

    private void whenPostingTransferBalance() throws Exception {
        result = postJson("/api/v1/wallets/transfer", transferBalanceBody());
    }

    private void whenGettingWalletSummary() throws Exception {
        result = mockMvc.perform(get("/api/v1/wallets/students/{studentId}/summary", studentId)
                .principal(authentication(COGNITO_SUBJECT))
                .param("desde", "2026-06-01")
                .param("hasta", "2026-06-14"));
    }

    private void whenLoadingBalanceWithoutAuthenticationFails() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> walletController.loadBalance(loadControllerRequest(), null));
    }

    private void whenTransferBalanceWithInvalidAuthenticationFails() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("subject", null);
        assertThrows(SecurityException.class,
                () -> walletController.transferBalance(transferControllerRequest(), authentication));
    }

    private void thenResponseIsOk() throws Exception {
        result.andExpect(status().isOk());
    }

    private void thenResponseIsBadRequest() throws Exception {
        result.andExpect(status().isBadRequest());
    }

    private void thenLoadBalanceWasDelegated() {
        verify(loadBalanceToChild).execute(
                new LoadBalanceToChildRequest(COGNITO_SUBJECT, studentId, amount));
    }

    private void thenTransferBalanceWasDelegated() {
        verify(planAccessGuard).requireTutorAdvanced(any());
        verify(transferBalanceBetweenChildren).execute(
                new TransferBalanceBetweenChildrenRequest(
                        COGNITO_SUBJECT,
                        fromStudentId,
                        toStudentId,
                        amount
                )
        );
    }

    private void thenTransferBalanceWasNotDelegated() {
        verifyNoInteractions(
                transferBalanceBetweenChildren,
                planAccessGuard
        );
    }

    private void thenWalletSummaryIsReturned() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoId").value(studentId.toString()))
                .andExpect(jsonPath("$.saldoActual").value(1250.00))
                .andExpect(jsonPath("$.periodo.desde").value("2026-06-01"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-06-14"))
                .andExpect(jsonPath("$.cantidadCompras").value(8));
    }

    private void thenWalletSummaryWasDelegated() {
        verify(getStudentWalletSummary).execute(summaryRequest());
    }

    private ResultActions postJson(String path, String requestBody) throws Exception {
        return mockMvc.perform(post(path)
                .principal(authentication(COGNITO_SUBJECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private String loadBalanceBody() {
        return String.format(
                "{\"studentId\":\"%s\",\"amount\":%s}",
                studentId,
                amount
        );
    }

    private String transferBalanceBody() {
        return String.format(
                "{\"fromStudentId\":%s,\"toStudentId\":%s,\"amount\":%s}",
                jsonValue(fromStudentId),
                jsonValue(toStudentId),
                amount
        );
    }

    private String jsonValue(UUID value) {
        return value == null ? null : "\"" + value + "\"";
    }

    private WalletController.LoadBalanceRequest loadControllerRequest() {
        return new WalletController.LoadBalanceRequest(studentId, amount);
    }

    private WalletController.TransferBalanceRequest transferControllerRequest() {
        return new WalletController.TransferBalanceRequest(fromStudentId, toStudentId, amount);
    }

    private GetStudentWalletSummaryRequest summaryRequest() {
        return new GetStudentWalletSummaryRequest(
                COGNITO_SUBJECT,
                studentId,
                Optional.of(LocalDate.of(2026, 6, 1)),
                Optional.of(LocalDate.of(2026, 6, 14))
        );
    }

    private GetStudentWalletSummaryResponse summaryResponse() {
        return new GetStudentWalletSummaryResponse(
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
        );
    }

    private Authentication authentication(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
