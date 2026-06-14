package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.MovimientoStock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository {
    MovimientoStock save(MovimientoStock movement);
    List<MovimientoStock> findByInventoryId(UUID inventoryId);
    List<MovimientoStock> findByBuffetAndDateRange(
            UUID buffetId,
            LocalDateTime start,
            LocalDateTime end);
}
