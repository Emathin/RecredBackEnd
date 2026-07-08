package com.recred.backend.unit.application;

import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.ListKiosqueroOrdersRequest;
import com.recred.backend.application.ListKiosqueroOrdersResponse;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListKiosqueroOrdersTest {

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final ListKiosqueroOrders useCase = new ListKiosqueroOrders(purchaseRepository);
    private UUID buffetId;
    private UUID pickupSlotId;
    private LocalDate pickupDate;
    private List<EstadoCompra> statuses;
    private ListKiosqueroOrdersResponse response;
    private IllegalArgumentException exception;

    @Test
    void execute_WhenFiltersAreMissing_UsesActiveStatusesAndDefaultLimit() {
        givenBuffet();
        givenOrdersFound();

        whenListingOrders(defaultRequest());

        thenNoOrdersAreReturned();
        thenOrdersWereRequestedWith(EstadoCompra.activeTrackingStatuses(), null, null, 100);
    }

    @Test
    void execute_WhenStatusesAreEmpty_UsesActiveStatuses() {
        givenBuffet();
        givenOrdersFound();

        whenListingOrders(requestWithStatuses(List.of()));

        thenOrdersWereRequestedWith(EstadoCompra.activeTrackingStatuses(), null, null, 100);
    }

    @Test
    void execute_WhenFiltersAreProvided_UsesProvidedFilters() {
        givenBuffet();
        givenOrderFilters();
        givenOrdersFound();

        whenListingOrders(requestWithFilters(25));

        thenOrdersWereRequestedWith(statuses, pickupDate, pickupSlotId, 25);
    }

    @Test
    void execute_WhenLimitIsTooHigh_CapsLimit() {
        givenBuffet();
        givenOrderFilters();
        givenOrdersFound();

        whenListingOrders(requestWithFilters(250));

        thenOrdersWereRequestedWith(statuses, pickupDate, pickupSlotId, 100);
    }

    @Test
    void execute_WhenLimitIsNotPositive_UsesDefaultLimit() {
        givenBuffet();
        givenOrderFilters();
        givenOrdersFound();

        whenListingOrders(requestWithFilters(0));

        thenOrdersWereRequestedWith(statuses, pickupDate, pickupSlotId, 100);
    }

    @Test
    void execute_WhenRequestIsNull_ThrowsException() {
        whenListingOrdersWith(null);

        thenExceptionMessageIs("request is required");
        thenOrdersWereNotRequested();
    }

    @Test
    void execute_WhenBuffetIdIsNull_ThrowsException() {
        whenListingOrdersWith(requestWithoutBuffet());

        thenExceptionMessageIs("buffetId is required");
        thenOrdersWereNotRequested();
    }

    private void givenBuffet() {
        buffetId = randomUUID();
    }

    private void givenOrderFilters() {
        pickupSlotId = randomUUID();
        pickupDate = LocalDate.of(2026, 6, 8);
        statuses = List.of(EstadoCompra.CANCELADO);
    }

    private void givenOrdersFound() {
        when(purchaseRepository.findKiosqueroOrders(
                any(),
                any(),
                any(),
                any(),
                anyInt()
        )).thenReturn(List.of());
    }

    private void whenListingOrders(ListKiosqueroOrdersRequest request) {
        response = useCase.execute(request);
    }

    private void whenListingOrdersWith(ListKiosqueroOrdersRequest request) {
        Executable execution = () -> useCase.execute(request);

        exception = assertThrows(IllegalArgumentException.class, execution);
    }

    private void thenNoOrdersAreReturned() {
        assertEquals(List.of(), response.orders());
    }

    private void thenOrdersWereRequestedWith(
            List<EstadoCompra> expectedStatuses,
            LocalDate expectedDate,
            UUID expectedPickupSlotId,
            int expectedLimit
    ) {
        verify(purchaseRepository).findKiosqueroOrders(
                buffetId,
                expectedStatuses,
                expectedDate,
                expectedPickupSlotId,
                expectedLimit
        );
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenOrdersWereNotRequested() {
        verifyNoInteractions(purchaseRepository);
    }

    private ListKiosqueroOrdersRequest defaultRequest() {
        return new ListKiosqueroOrdersRequest(buffetId, null, null, null, null);
    }

    private ListKiosqueroOrdersRequest requestWithStatuses(List<EstadoCompra> statuses) {
        return new ListKiosqueroOrdersRequest(buffetId, statuses, null, null, null);
    }

    private ListKiosqueroOrdersRequest requestWithFilters(Integer limit) {
        return new ListKiosqueroOrdersRequest(
                buffetId,
                statuses,
                pickupDate,
                pickupSlotId,
                limit
        );
    }

    private ListKiosqueroOrdersRequest requestWithoutBuffet() {
        return new ListKiosqueroOrdersRequest(null, null, null, null, null);
    }
}
