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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class TutorGlobalDashboardControllerTest {

    private static final String COGNITO_SUBJECT = "tutor-subject";

    @Mock
    private GetTutorGlobalDashboard getTutorGlobalDashboard;
    @Mock
    private UpdateTutorDashboardConfig updateTutorDashboardConfig;
    @Mock
    private PlanAccessGuard planAccessGuard;

    private TutorGlobalDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new TutorGlobalDashboardController(
                getTutorGlobalDashboard,
                updateTutorDashboardConfig,
                planAccessGuard
        );
    }

    @Test
    void getGlobalDashboardShouldMapCompleteChildDashboard() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime transactionDate = LocalDateTime.of(2026, 7, 3, 10, 30);
        Authentication authentication = authentication();
        GetTutorGlobalDashboardRequest request =
                new GetTutorGlobalDashboardRequest(COGNITO_SUBJECT);
        when(getTutorGlobalDashboard.execute(request)).thenReturn(new GetTutorGlobalDashboardResponse(
                "tutor-1",
                new BigDecimal("1500.00"),
                new BigDecimal("250.00"),
                "{\"cards\":[\"balance\"]}",
                List.of(completeChild(studentId, transactionDate)),
                "AVANZADO"
        ));

        ResponseEntity<TutorGlobalDashboardDTO> response =
                controller.getGlobalDashboard(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("tutor-1", response.getBody().tutorId());
        assertEquals(new BigDecimal("1500.00"), response.getBody().totalBalance());
        assertEquals("AVANZADO", response.getBody().plan());
        assertEquals(1, response.getBody().children().size());

        TutorGlobalDashboardDTO.ChildDashboardDTO child = response.getBody().children().get(0);
        assertEquals(studentId, child.studentId());
        assertEquals("Juana Perez", child.studentName());
        assertEquals("MENSUAL", child.budget().period());
        assertEquals("ORO", child.health().rewardLevel());
        assertEquals("purchase-1", child.todayPickups().get(0).id());
        assertEquals(transactionDate, child.recentTransactions().get(0).date());
        assertEquals("Cargar saldo", child.smartAction().title());
        verify(planAccessGuard).requireTutorIntermediate(authentication);
        verify(getTutorGlobalDashboard).execute(request);
    }

    @Test
    void getGlobalDashboardShouldKeepNullableSectionsAsNull() {
        UUID studentId = UUID.randomUUID();
        Authentication authentication = authentication();
        GetTutorGlobalDashboardRequest request =
                new GetTutorGlobalDashboardRequest(COGNITO_SUBJECT);
        when(getTutorGlobalDashboard.execute(request)).thenReturn(new GetTutorGlobalDashboardResponse(
                "tutor-1",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                List.of(minimalChild(studentId)),
                "GRATUITO"
        ));

        ResponseEntity<TutorGlobalDashboardDTO> response =
                controller.getGlobalDashboard(authentication);

        TutorGlobalDashboardDTO.ChildDashboardDTO child = response.getBody().children().get(0);
        assertEquals(studentId, child.studentId());
        assertNull(child.budget());
        assertNull(child.health());
        assertEquals(List.of(), child.todayPickups());
        assertEquals(List.of(), child.recentTransactions());
        assertNull(child.smartAction());
        verify(planAccessGuard).requireTutorIntermediate(authentication);
        verify(getTutorGlobalDashboard).execute(request);
    }

    @Test
    void getGlobalDashboardWithoutAuthenticationShouldThrowException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getGlobalDashboard(null));
    }

    @Test
    void updateDashboardConfigShouldDelegateWithAuthenticatedSubject() {
        String config = "{\"cards\":[\"children\",\"health\"]}";
        Authentication authentication = authentication();

        ResponseEntity<Void> response =
                controller.updateDashboardConfig(authentication, config);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(planAccessGuard).requireTutorIntermediate(authentication);
        verify(updateTutorDashboardConfig).execute(
                new UpdateTutorDashboardConfigRequest(COGNITO_SUBJECT, config));
    }

    @Test
    void updateDashboardConfigWithoutAuthenticationShouldThrowException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.updateDashboardConfig(null, "{}"));
    }

    private GetTutorGlobalDashboardResponse.ChildDashboardResponse completeChild(
            UUID studentId,
            LocalDateTime transactionDate
    ) {
        return new GetTutorGlobalDashboardResponse.ChildDashboardResponse(
                studentId,
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
                        "ORO",
                        80,
                        20,
                        "Semana saludable"
                ),
                List.of(new GetTutorGlobalDashboardResponse.ScheduledPickupResponse(
                        "purchase-1",
                        "Menu saludable",
                        "Primer recreo"
                )),
                List.of(new GetTutorGlobalDashboardResponse.TransactionSummaryResponse(
                        "tx-1",
                        "Compra buffet",
                        transactionDate,
                        new BigDecimal("250.00"),
                        "shopping-bag"
                )),
                new GetTutorGlobalDashboardResponse.SmartActionResponse(
                        "Cargar saldo",
                        "Saldo bajo",
                        "TOP_UP",
                        "student:" + studentId
                )
        );
    }

    private GetTutorGlobalDashboardResponse.ChildDashboardResponse minimalChild(UUID studentId) {
        return new GetTutorGlobalDashboardResponse.ChildDashboardResponse(
                studentId,
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

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
        return authentication;
    }
}
