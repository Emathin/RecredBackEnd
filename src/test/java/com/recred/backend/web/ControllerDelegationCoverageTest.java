package com.recred.backend.web;

import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.web.BudgetController;
import com.recred.backend.infrastructure.web.CheckBudgetDatesRequest;
import com.recred.backend.infrastructure.web.CheckBudgetDatesResponse;
import com.recred.backend.infrastructure.web.PurchaseRecommendationController;
import com.recred.backend.infrastructure.web.SalesController;
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
    private GeneratePurchaseRecommendation generatePurchaseRecommendation;
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

    @Test
    void purchaseRecommendationControllerShouldDelegateTodayAndTomorrow() {
        UUID studentId = UUID.randomUUID();
        NotificacionEntity notification = new NotificacionEntity();
        when(generatePurchaseRecommendation.findSuggestionForToday(studentId))
                .thenReturn(notification);
        PurchaseRecommendationController controller =
                new PurchaseRecommendationController(generatePurchaseRecommendation);

        NotificacionEntity today = controller.getTodayPurchaseRecommendation(studentId);
        NotificacionEntity tomorrow = controller.getTomorrowPurchaseRecommendation(studentId);

        assertSame(notification, today);
        assertSame(notification, tomorrow);
        verify(generatePurchaseRecommendation, times(2))
                .findSuggestionForToday(studentId);
    }

    @Test
    void salesControllerShouldDelegateToProductStatisticsUseCase() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductSaleStatistics statistics = new ProductSaleStatistics();
        when(getProductStatistics.execute(productId, kiosqueroId)).thenReturn(statistics);
        SalesController controller = new SalesController(getProductStatistics);

        ResponseEntity<ProductSaleStatistics> response =
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
        when(checkBudgetDates.execute(studentId, request)).thenReturn(expected);
        BudgetController controller = new BudgetController(
                createBudget,
                updateBudget,
                disableBudget,
                getActiveBudgetByStudent,
                updateBudgetRules,
                checkBudgetDates
        );

        ResponseEntity<CheckBudgetDatesResponse> response =
                controller.checkDates(studentId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }
}
