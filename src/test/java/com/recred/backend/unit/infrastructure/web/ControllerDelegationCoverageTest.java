package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.application.CheckBudgetDatesRequest;
import com.recred.backend.application.CheckBudgetDatesResponse;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.FindPurchaseSuggestionForToday;
import com.recred.backend.application.FindPurchaseSuggestionForTomorrow;
import com.recred.backend.application.FindPurchaseSuggestionRequest;
import com.recred.backend.application.FindPurchaseSuggestionResponse;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.GetProductStatisticsRequest;
import com.recred.backend.application.ProductSaleStatisticsResponse;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerDelegationCoverageTest {

    @Mock
    private FindPurchaseSuggestionForToday findPurchaseSuggestionForToday;
    @Mock
    private FindPurchaseSuggestionForTomorrow findPurchaseSuggestionForTomorrow;
    @Mock
    private GetProductStatistics getProductStatistics;
    @Mock
    private CreateBudget createBudget;
    @Mock
    private UpdateBudget updateBudget;
    @Mock
    private DisableBudget disableBudget;
    @Mock
    private GetActiveBudgetByStudent getActiveBudgetByStudent;
    @Mock
    private UpdateBudgetRules updateBudgetRules;
    @Mock
    private CheckBudgetDates checkBudgetDates;
    @Mock
    private TutorAccessGuard tutorAccessGuard;
    @Mock
    private org.springframework.security.core.Authentication authentication;

    @Test
    void purchaseRecommendationControllerShouldDelegateTodayAndTomorrow() {
        String cognitoSubject = "test-sub";
        org.springframework.security.core.Authentication auth = 
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(cognitoSubject);

        FindPurchaseSuggestionResponse response = new FindPurchaseSuggestionResponse(
                UUID.randomUUID(), com.recred.backend.domain.models.TipoNotificacion.SALDO_BAJO, 
                "Title", "Msg", false, null, null);
        
        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);

        when(findPurchaseSuggestionForToday.execute(request)).thenReturn(response);
        when(findPurchaseSuggestionForTomorrow.execute(request)).thenReturn(response);
        
        PurchaseRecommendationController controller =
                new PurchaseRecommendationController(findPurchaseSuggestionForToday, findPurchaseSuggestionForTomorrow);

        FindPurchaseSuggestionResponse today = controller.getTodayPurchaseRecommendation(auth);
        FindPurchaseSuggestionResponse tomorrow = controller.getTomorrowPurchaseRecommendation(auth);

        assertSame(response, today);
        assertSame(response, tomorrow);
        verify(findPurchaseSuggestionForToday, times(1)).execute(request);
        verify(findPurchaseSuggestionForTomorrow, times(1)).execute(request);
    }

    @Test
    void salesControllerShouldDelegateToProductStatisticsUseCase() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductSaleStatisticsResponse statistics = new ProductSaleStatisticsResponse(
                productId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(getProductStatistics.execute(new GetProductStatisticsRequest(productId, kiosqueroId)))
                .thenReturn(statistics);
        SalesController controller = new SalesController(getProductStatistics);

        ResponseEntity<ProductSaleStatisticsResponse> response =
                controller.getProductStatistics(kiosqueroId, productId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(statistics, response.getBody());
    }

    @Test
    void budgetControllerShouldDelegateCheckDates() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("2026-06-16"),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(productId, 1))
        );
        CheckBudgetDatesResponse expected = new CheckBudgetDatesResponse(
                List.of(new CheckBudgetDatesResponse.DateBudgetStatus(
                        "2026-06-16",
                        false,
                        null
                ))
        );
        when(checkBudgetDates.execute(request.withStudentId(studentId))).thenReturn(expected);
        BudgetController controller = new BudgetController(
                createBudget,
                updateBudget,
                disableBudget,
                getActiveBudgetByStudent,
                updateBudgetRules,
                checkBudgetDates,
                tutorAccessGuard
        );

        ResponseEntity<CheckBudgetDatesResponse> response =
                controller.checkDates(studentId, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }
}
