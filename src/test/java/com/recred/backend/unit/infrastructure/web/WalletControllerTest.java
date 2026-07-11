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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import com.recred.backend.infrastructure.web.StudentWalletSummaryResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    private ResponseEntity<?> result;
    private UUID studentId;
    private UUID fromStudentId;
    private UUID toStudentId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        // No setup needed
    }

    @Test
    void givenLoadBalanceRequestWhenLoadingBalanceThenReturns200AndDelegatesToUseCase() {
        givenLoadBalanceRequest();

        whenLoadingBalance();

        thenResponseIsOk();
        thenLoadBalanceWasDelegated();
    }

    @Test
    void givenTransferBalanceRequestWhenTransferringBalanceThenReturns200AndDelegatesToUseCase() {
        givenTransferBalanceRequest();

        whenTransferringBalance();

        thenResponseIsOk();
        thenTransferBalanceWasDelegated();
    }

    @Test
    void givenTransferBalanceRequestToSameStudentWhenTransferringBalanceThenThrowsIllegalArgumentException() {
        givenTransferBalanceRequestToSameStudent();

        assertThrows(IllegalArgumentException.class, this::whenTransferringBalance);
        thenTransferBalanceWasNotDelegated();
    }

    @Test
    void givenWalletSummaryRequestWhenGettingWalletSummaryThenReturns200AndDelegatesToUseCase() {
        givenWalletSummary();

        whenGettingWalletSummary();

        thenWalletSummaryIsReturned();
        thenWalletSummaryWasDelegated();
    }

    @Test
    void givenLoadBalanceRequestWithoutAuthenticationWhenLoadingBalanceThenThrows() {
        givenLoadBalanceRequest();

        whenLoadingBalanceWithoutAuthenticationFails();
    }

    @Test
    void givenTransferRequestWithInvalidAuthenticationPrincipalWhenTransferringThenThrows() {
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

    private void whenLoadingBalance() {
        result = walletController.loadBalance(loadControllerRequest(), authentication(COGNITO_SUBJECT));
    }

    private void whenTransferringBalance() {
        result = walletController.transferBalance(transferControllerRequest(), authentication(COGNITO_SUBJECT));
    }

    private void whenGettingWalletSummary() {
        result = walletController.getStudentWalletSummary(
                studentId,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 14),
                authentication(COGNITO_SUBJECT)
        );
    }

    private void whenLoadingBalanceWithoutAuthenticationFails() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> walletController.loadBalance(loadControllerRequest(), null));
    }

    private void whenTransferBalanceWithInvalidAuthenticationFails() {
        org.springframework.security.core.Authentication authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("subject", null);
        assertThrows(SecurityException.class,
                () -> walletController.transferBalance(transferControllerRequest(), authentication));
    }

    private void thenResponseIsOk() {
        assertEquals(200, result.getStatusCode().value());
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

    private void thenWalletSummaryIsReturned() {
        assertEquals(200, result.getStatusCode().value());
        StudentWalletSummaryResponseDTO body = (StudentWalletSummaryResponseDTO) result.getBody();
        assertEquals(studentId, body.alumnoId());
        assertEquals(new BigDecimal("1250.00"), body.saldoActual());
        assertEquals(LocalDate.of(2026, 6, 1), body.periodo().desde());
        assertEquals(LocalDate.of(2026, 6, 14), body.periodo().hasta());
        assertEquals(8, body.cantidadCompras());
    }

    private void thenWalletSummaryWasDelegated() {
        verify(getStudentWalletSummary).execute(summaryRequest());
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

    private org.springframework.security.core.Authentication authentication(String subject) {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);
    }
}
