package com.recred.backend.unit.application;

import com.recred.backend.application.GetStockMovements;
import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetStockMovementsTest {

    private final InventoryRepository inventoryRepository = mock(InventoryRepository.class);
    private final StockMovementRepository stockMovementRepository =
            mock(StockMovementRepository.class);
    private final GetStockMovements useCase = new GetStockMovements(
            inventoryRepository,
            stockMovementRepository
    );

    @Test
    void execute_ShouldReturnMovementsForInventory() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        Inventario inventory = new Inventario();
        inventory.setId(inventoryId);
        MovimientoStock movement = new MovimientoStock(
                UUID.randomUUID(),
                inventoryId,
                TipoMovimientoStock.AJUSTE,
                4,
                8,
                12,
                "Ajuste",
                UUID.randomUUID(),
                null,
                LocalDateTime.now()
        );

        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(stockMovementRepository.findByInventoryId(inventoryId))
                .thenReturn(List.of(movement));

        List<MovimientoStock> result = useCase.execute(buffetId, productId);

        assertEquals(List.of(movement), result);
        verify(stockMovementRepository).findByInventoryId(inventoryId);
    }

    @Test
    void execute_WhenInventoryDoesNotExist_ShouldThrowException() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class,
                () -> useCase.execute(buffetId, productId));
    }
}
