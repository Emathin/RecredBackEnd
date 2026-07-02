package com.recred.backend.unit.application;

import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.GetStockMovementsRequest;
import com.recred.backend.application.GetStockMovementsResponse;
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
    private UUID buffetId;
    private UUID productId;
    private UUID inventoryId;
    private MovimientoStock movement;
    private GetStockMovementsResponse response;

    @Test
    void execute_WhenInventoryExists_ShouldReturnMovementResponses() {
        givenInventoryIdentifiers();
        givenInventoryExists();
        givenInventoryHasMovements();

        whenGettingStockMovements();

        thenMovementResponsesAreReturned();
        thenMovementsWereLoadedByInventory();
    }

    @Test
    void execute_WhenInventoryDoesNotExist_ShouldThrowException() {
        givenInventoryIdentifiers();
        givenInventoryDoesNotExist();

        assertThrows(InventoryNotFoundException.class, this::whenGettingStockMovements);
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new GetStockMovementsRequest(null, UUID.randomUUID())));
    }

    @Test
    void execute_WhenProductIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new GetStockMovementsRequest(UUID.randomUUID(), null)));
    }

    private void givenInventoryIdentifiers() {
        buffetId = UUID.randomUUID();
        productId = UUID.randomUUID();
        inventoryId = UUID.randomUUID();
    }

    private void givenInventoryExists() {
        Inventario inventory = new Inventario();
        inventory.setId(inventoryId);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
    }

    private void givenInventoryDoesNotExist() {
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());
    }

    private void givenInventoryHasMovements() {
        movement = new MovimientoStock(
                UUID.randomUUID(),
                inventoryId,
                TipoMovimientoStock.AJUSTE,
                4,
                8,
                12,
                "Ajuste",
                UUID.randomUUID(),
                null,
                LocalDateTime.of(2026, 6, 8, 10, 0)
        );
        when(stockMovementRepository.findByInventoryId(inventoryId))
                .thenReturn(List.of(movement));
    }

    private void whenGettingStockMovements() {
        response = useCase.execute(new GetStockMovementsRequest(buffetId, productId));
    }

    private void thenMovementResponsesAreReturned() {
        GetStockMovementsResponse.StockMovementResponse stockMovement =
                response.movements().get(0);
        assertEquals(movement.id(), stockMovement.id());
        assertEquals(inventoryId, stockMovement.inventarioId());
        assertEquals("AJUSTE", stockMovement.tipo());
        assertEquals(4, stockMovement.cantidad());
        assertEquals("Ajuste", stockMovement.motivo());
    }

    private void thenMovementsWereLoadedByInventory() {
        verify(stockMovementRepository).findByInventoryId(inventoryId);
    }
}
