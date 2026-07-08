package com.recred.backend.unit.infrastructure.web;

import java.time.LocalDate;

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
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.web.BudgetController;
import com.recred.backend.infrastructure.web.PurchaseRecommendationController;
import com.recred.backend.infrastructure.web.SalesController;
import com.recred.backend.infrastructure.web.TutorAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
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
    void purchaseRecommendationControllerShouldDelegateToday() {
        String cognitoSubject = "test-sub";
        Authentication authentication = givenAuthenticatedUser(cognitoSubject);
        FindPurchaseSuggestionResponse expectedResponse = givenTodayPurchaseSuggestionFor(cognitoSubject);
        PurchaseRecommendationController controller = givenPurchaseRecommendationController();

        FindPurchaseSuggestionResponse response =
                whenGettingTodayPurchaseRecommendation(controller, authentication);

        thenTodayPurchaseRecommendationIsDelegated(cognitoSubject, expectedResponse, response);
    }

    @Test
    void purchaseRecommendationControllerShouldDelegateTomorrow() {
        String cognitoSubject = "test-sub";
        Authentication authentication = givenAuthenticatedUser(cognitoSubject);
        FindPurchaseSuggestionResponse expectedResponse = givenTomorrowPurchaseSuggestionFor(cognitoSubject);
        PurchaseRecommendationController controller = givenPurchaseRecommendationController();

        FindPurchaseSuggestionResponse response =
                whenGettingTomorrowPurchaseRecommendation(controller, authentication);

        thenTomorrowPurchaseRecommendationIsDelegated(cognitoSubject, expectedResponse, response);
    }

    @Test
    void salesControllerShouldDelegateToProductStatisticsUseCase() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductSaleStatisticsResponse statistics = givenProductStatistics(productId, kiosqueroId);
        SalesController controller = givenSalesController();

        ResponseEntity<ProductSaleStatisticsResponse> response = whenGettingProductStatistics(
                controller,
                kiosqueroId,
                productId
        );

        thenProductStatisticsAreReturned(response, statistics);
    }

    @Test
    void budgetControllerShouldDelegateCheckDates() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of(LocalDate.of(2026, 6, 16)),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(productId, 1))
        );
        CheckBudgetDatesResponse expected = new CheckBudgetDatesResponse(
                List.of(new CheckBudgetDatesResponse.DateBudgetStatus(
                        LocalDate.of(2026, 6, 16),
                        false,
                        null
                ))
        );
        givenBudgetDatesChecked(studentId, request, expected);
        BudgetController controller = givenBudgetController();

        ResponseEntity<CheckBudgetDatesResponse> response = whenCheckingBudgetDates(
                controller,
                studentId,
                request
        );

        thenBudgetDatesAreReturned(response, expected);
    }

    private Authentication givenAuthenticatedUser(String cognitoSubject) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(cognitoSubject);
        return authentication;
    }

    private FindPurchaseSuggestionResponse givenTodayPurchaseSuggestionFor(String cognitoSubject) {
        FindPurchaseSuggestionResponse response = new FindPurchaseSuggestionResponse(
                UUID.randomUUID(),
                TipoNotificacion.SALDO_BAJO,
                "Title",
                "Msg",
                false,
                null,
                null
        );
        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);

        when(findPurchaseSuggestionForToday.execute(request)).thenReturn(response);

        return response;
    }

    private FindPurchaseSuggestionResponse givenTomorrowPurchaseSuggestionFor(String cognitoSubject) {
        FindPurchaseSuggestionResponse response = new FindPurchaseSuggestionResponse(
                UUID.randomUUID(),
                TipoNotificacion.SALDO_BAJO,
                "Title",
                "Msg",
                false,
                null,
                null
        );
        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);

        when(findPurchaseSuggestionForTomorrow.execute(request)).thenReturn(response);

        return response;
    }

    private PurchaseRecommendationController givenPurchaseRecommendationController() {
        return new PurchaseRecommendationController(
                findPurchaseSuggestionForToday,
                findPurchaseSuggestionForTomorrow
        );
    }

    private FindPurchaseSuggestionResponse whenGettingTodayPurchaseRecommendation(
            PurchaseRecommendationController controller,
            Authentication authentication
    ) {
        return controller.getTodayPurchaseRecommendation(authentication);
    }

    private FindPurchaseSuggestionResponse whenGettingTomorrowPurchaseRecommendation(
            PurchaseRecommendationController controller,
            Authentication authentication
    ) {
        return controller.getTomorrowPurchaseRecommendation(authentication);
    }

    private void thenTodayPurchaseRecommendationIsDelegated(
            String cognitoSubject,
            FindPurchaseSuggestionResponse expectedResponse,
            FindPurchaseSuggestionResponse response
    ) {
        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);

        assertSame(expectedResponse, response);
        verify(findPurchaseSuggestionForToday).execute(request);
    }

    private void thenTomorrowPurchaseRecommendationIsDelegated(
            String cognitoSubject,
            FindPurchaseSuggestionResponse expectedResponse,
            FindPurchaseSuggestionResponse response
    ) {
        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);

        assertSame(expectedResponse, response);
        verify(findPurchaseSuggestionForTomorrow).execute(request);
    }

    private ProductSaleStatisticsResponse givenProductStatistics(UUID productId, UUID kiosqueroId) {
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
        return statistics;
    }

    private SalesController givenSalesController() {
        return new SalesController(getProductStatistics);
    }

    private ResponseEntity<ProductSaleStatisticsResponse> whenGettingProductStatistics(
            SalesController controller,
            UUID kiosqueroId,
            UUID productId
    ) {
        return controller.getProductStatistics(kiosqueroId, productId);
    }

    private void thenProductStatisticsAreReturned(
            ResponseEntity<ProductSaleStatisticsResponse> response,
            ProductSaleStatisticsResponse statistics
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(statistics, response.getBody());
    }

    private void givenBudgetDatesChecked(
            UUID studentId,
            CheckBudgetDatesRequest request,
            CheckBudgetDatesResponse expected
    ) {
        when(checkBudgetDates.execute(request.withStudentId(studentId))).thenReturn(expected);
    }

    private BudgetController givenBudgetController() {
        return new BudgetController(
                createBudget,
                updateBudget,
                disableBudget,
                getActiveBudgetByStudent,
                updateBudgetRules,
                checkBudgetDates,
                tutorAccessGuard
        );
    }

    private ResponseEntity<CheckBudgetDatesResponse> whenCheckingBudgetDates(
            BudgetController controller,
            UUID studentId,
            CheckBudgetDatesRequest request
    ) {
        return controller.checkDates(studentId, request, authentication);
    }

    private void thenBudgetDatesAreReturned(
            ResponseEntity<CheckBudgetDatesResponse> response,
            CheckBudgetDatesResponse expected
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }
}
