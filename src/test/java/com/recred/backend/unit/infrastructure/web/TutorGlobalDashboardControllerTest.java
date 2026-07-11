package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.GetTutorGlobalDashboardRequest;
import com.recred.backend.application.GetTutorGlobalDashboardResponse;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.application.UpdateTutorDashboardConfigRequest;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardController;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class TutorGlobalDashboardControllerTest {

    private static final String COGNITO_SUBJECT = "tutor-subject";
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final LocalDateTime TRANSACTION_DATE =
            LocalDateTime.of(2026, 7, 3, 10, 30);

    private GetTutorGlobalDashboard getTutorGlobalDashboard;
    private UpdateTutorDashboardConfig updateTutorDashboardConfig;
    private PlanAccessGuard planAccessGuard;
    private TutorGlobalDashboardController controller;
    private Authentication authentication;
    private ResponseEntity<?> response;

    @BeforeEach
    void setUp() {
        getTutorGlobalDashboard = mock(GetTutorGlobalDashboard.class);
        updateTutorDashboardConfig = mock(UpdateTutorDashboardConfig.class);
        planAccessGuard = mock(PlanAccessGuard.class);
        controller = new TutorGlobalDashboardController(
                getTutorGlobalDashboard,
                updateTutorDashboardConfig,
                planAccessGuard
        );
    }

    @Test
    void givenValidRequestWhenGettingGlobalDashboardThenMapsCompleteChildDashboard() {
        givenAuthenticatedUser();
        givenDashboardWithCompleteChild();

        whenGettingGlobalDashboard();

        thenStatusIsOk();
        thenTutorIdIs("tutor-1");
        thenTotalBalanceIs(new BigDecimal("1500.00"));
        thenPlanIs("AVANZADO");
        thenChildrenCountIs(1);
        thenChildStudentIdIs(STUDENT_ID);
        thenChildNameIs("Juana Perez");
        thenChildBudgetPeriodIs("MENSUAL");
        thenChildHealthRewardLevelIs("ORO");
        thenChildPickupIdIs("purchase-1");
        thenChildTransactionDateIs(TRANSACTION_DATE);
        thenChildSmartActionTitleIs("Cargar saldo");
        thenPlanAccessWasGuarded();
        thenDashboardWasDelegated();
    }

    @Test
    void givenMissingDataWhenGettingGlobalDashboardThenKeepsNullableSectionsAsNull() {
        givenAuthenticatedUser();
        givenDashboardWithMinimalChild();

        whenGettingGlobalDashboard();

        thenChildStudentIdIs(STUDENT_ID);
        thenChildBudgetIsNull();
        thenChildHealthIsNull();
        thenChildPickupsAreEmpty();
        thenChildTransactionsAreEmpty();
        thenChildSmartActionIsNull();
        thenPlanAccessWasGuarded();
        thenDashboardWasDelegated();
    }

    @Test
    void givenNoAuthenticationWhenGettingGlobalDashboardThenThrowsException() {
        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> controller.getGlobalDashboard(null));
    }

    @Test
    void givenValidConfigWhenUpdatingDashboardThenDelegates() {
        givenAuthenticatedUser();
        String config = "{\"cards\":[\"children\",\"health\"]}";

        whenUpdatingDashboardConfig(config);

        thenStatusIsOk();
        thenPlanAccessWasGuarded();
        thenConfigUpdateWasDelegated(config);
    }

    @Test
    void givenNoAuthenticationWhenUpdatingDashboardThenThrowsException() {
        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> controller.updateDashboardConfig(null, "{}"));
    }

    private void givenAuthenticatedUser() {
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
    }

    private void givenDashboardWithCompleteChild() {
        GetTutorGlobalDashboardRequest request =
                new GetTutorGlobalDashboardRequest(COGNITO_SUBJECT);
        when(getTutorGlobalDashboard.execute(request))
                .thenReturn(new GetTutorGlobalDashboardResponse(
                        "tutor-1",
                        new BigDecimal("1500.00"),
                        new BigDecimal("250.00"),
                        "{\"cards\":[\"balance\"]}",
                        List.of(completeChild()),
                        "AVANZADO"
                ));
    }

    private void givenDashboardWithMinimalChild() {
        GetTutorGlobalDashboardRequest request =
                new GetTutorGlobalDashboardRequest(COGNITO_SUBJECT);
        when(getTutorGlobalDashboard.execute(request))
                .thenReturn(new GetTutorGlobalDashboardResponse(
                        "tutor-1",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        List.of(minimalChild()),
                        "GRATUITO"
                ));
    }

    private void whenGettingGlobalDashboard() {
        response = controller.getGlobalDashboard(authentication);
    }

    private void whenUpdatingDashboardConfig(String config) {
        response = controller.updateDashboardConfig(
                authentication, config);
    }

    private TutorGlobalDashboardDTO dashboardBody() {
        return (TutorGlobalDashboardDTO) response.getBody();
    }

    private TutorGlobalDashboardDTO.ChildDashboardDTO firstChild() {
        return dashboardBody().children().get(0);
    }

    private void thenStatusIsOk() {
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void thenTutorIdIs(String expected) {
        assertEquals(expected, dashboardBody().tutorId());
    }

    private void thenTotalBalanceIs(BigDecimal expected) {
        assertEquals(expected, dashboardBody().totalBalance());
    }

    private void thenPlanIs(String expected) {
        assertEquals(expected, dashboardBody().plan());
    }

    private void thenChildrenCountIs(int expected) {
        assertEquals(expected, dashboardBody().children().size());
    }

    private void thenChildStudentIdIs(UUID expected) {
        assertEquals(expected, firstChild().studentId());
    }

    private void thenChildNameIs(String expected) {
        assertEquals(expected, firstChild().studentName());
    }

    private void thenChildBudgetPeriodIs(String expected) {
        assertEquals(expected, firstChild().budget().period());
    }

    private void thenChildHealthRewardLevelIs(String expected) {
        assertEquals(expected, firstChild().health().rewardLevel());
    }

    private void thenChildPickupIdIs(String expected) {
        assertEquals(expected, firstChild().todayPickups().get(0).id());
    }

    private void thenChildTransactionDateIs(LocalDateTime expected) {
        assertEquals(
                expected, firstChild().recentTransactions().get(0).date());
    }

    private void thenChildSmartActionTitleIs(String expected) {
        assertEquals(expected, firstChild().smartAction().title());
    }

    private void thenChildBudgetIsNull() {
        assertNull(firstChild().budget());
    }

    private void thenChildHealthIsNull() {
        assertNull(firstChild().health());
    }

    private void thenChildPickupsAreEmpty() {
        assertEquals(List.of(), firstChild().todayPickups());
    }

    private void thenChildTransactionsAreEmpty() {
        assertEquals(List.of(), firstChild().recentTransactions());
    }

    private void thenChildSmartActionIsNull() {
        assertNull(firstChild().smartAction());
    }

    private void thenPlanAccessWasGuarded() {
        verify(planAccessGuard)
                .requireTutorIntermediate(authentication);
    }

    private void thenDashboardWasDelegated() {
        verify(getTutorGlobalDashboard).execute(
                new GetTutorGlobalDashboardRequest(COGNITO_SUBJECT));
    }

    private void thenConfigUpdateWasDelegated(String config) {
        verify(updateTutorDashboardConfig).execute(
                new UpdateTutorDashboardConfigRequest(
                        COGNITO_SUBJECT, config));
    }

    private GetTutorGlobalDashboardResponse.ChildDashboardResponse
            completeChild() {
        return new GetTutorGlobalDashboardResponse.ChildDashboardResponse(
                STUDENT_ID,
                "Juana Perez",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "https://images.test/juana.jpg",
                "Gasto estable",
                new GetTutorGlobalDashboardResponse.BudgetSummaryResponse(
                        "MENSUAL",
                        new BigDecimal("300.00"),
                        new BigDecimal("1200.00")
                ),
                new GetTutorGlobalDashboardResponse.HealthSummaryResponse(
                        "ORO", 80, 20, "Semana saludable"
                ),
                List.of(
                        new GetTutorGlobalDashboardResponse
                                .ScheduledPickupResponse(
                                "purchase-1",
                                "Menu saludable",
                                "Primer recreo"
                        )),
                List.of(
                        new GetTutorGlobalDashboardResponse
                                .TransactionSummaryResponse(
                                "tx-1",
                                "Compra buffet",
                                TRANSACTION_DATE,
                                new BigDecimal("250.00"),
                                "shopping-bag"
                        )),
                new GetTutorGlobalDashboardResponse.SmartActionResponse(
                        "Cargar saldo",
                        "Saldo bajo",
                        "TOP_UP",
                        "student:" + STUDENT_ID
                )
        );
    }

    private GetTutorGlobalDashboardResponse.ChildDashboardResponse
            minimalChild() {
        return new GetTutorGlobalDashboardResponse.ChildDashboardResponse(
                STUDENT_ID,
                "Pedro Perez",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null
        );
    }
}
