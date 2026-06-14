package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ListKiosqueroOrdersTest {

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final ListKiosqueroOrders useCase = new ListKiosqueroOrders(purchaseRepository);

    @Test
    void execute_WhenFiltersAreEmpty_ShouldUseActiveStatusesAndDefaultLimit() {
        UUID buffetId = UUID.randomUUID();

        useCase.execute(buffetId, null, null, null, null);

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

        useCase.execute(buffetId, statuses, date, slotId, 250);

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
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, null, null, null, null));
    }
}
