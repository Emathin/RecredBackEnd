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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListKiosqueroOrdersTest {

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final ListKiosqueroOrders useCase = new ListKiosqueroOrders(purchaseRepository);

    @Test
    void execute_WhenFiltersAreEmpty_ShouldUseActiveStatusesAndDefaultLimit() {
        UUID buffetId = UUID.randomUUID();
        ListKiosqueroOrdersRequest request = new ListKiosqueroOrdersRequest(
                buffetId,
                null,
                null,
                null,
                null
        );
        givenOrdersFound();

        ListKiosqueroOrdersResponse response = whenListingOrders(request);

        assertEquals(List.of(), response.orders());
        verify(purchaseRepository).findKiosqueroOrders(
                buffetId,
                List.of(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO
                ),
                null,
                null,
                100
        );
    }

    @Test
    void execute_WhenLimitIsTooHigh_ShouldCapLimit() {
        UUID buffetId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 8);
        List<EstadoCompra> statuses = List.of(EstadoCompra.CANCELADO);
        ListKiosqueroOrdersRequest request = new ListKiosqueroOrdersRequest(
                buffetId,
                statuses,
                date,
                slotId,
                250
        );
        givenOrdersFound();

        whenListingOrders(request);

        verify(purchaseRepository).findKiosqueroOrders(
                buffetId,
                statuses,
                date,
                slotId,
                100
        );
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        ListKiosqueroOrdersRequest request = new ListKiosqueroOrdersRequest(
                null,
                null,
                null,
                null,
                null
        );

        Executable execution = () -> whenListingOrders(request);

        assertThrows(IllegalArgumentException.class, execution);
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

    private ListKiosqueroOrdersResponse whenListingOrders(ListKiosqueroOrdersRequest request) {
        return useCase.execute(request);
    }
}
