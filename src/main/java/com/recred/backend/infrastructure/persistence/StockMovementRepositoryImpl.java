package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.repositories.StockMovementRepository;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.MovimientoStockEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StockMovementRepositoryImpl implements StockMovementRepository {

    private final SpringDataStockMovementRepository springDataRepository;

    public StockMovementRepositoryImpl(SpringDataStockMovementRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public MovimientoStock save(MovimientoStock movement) {
        MovimientoStockEntity entity = new MovimientoStockEntity();
        entity.setId(movement.id());
        entity.setInventario(inventoryReference(movement.inventarioId()));
        entity.setTipo(movement.tipo());
        entity.setCantidad(movement.cantidad());
        entity.setCantidadAnterior(movement.cantidadAnterior());
        entity.setCantidadNueva(movement.cantidadNueva());
        entity.setMotivo(movement.motivo());
        entity.setCompraId(movement.compraId());
        entity.setCreadoPorUsuarioEntity(userReference(movement.usuarioId()));
        return springDataRepository.save(entity).toDomain();
    }

    @Override
    public List<MovimientoStock> findByInventoryId(UUID inventoryId) {
        return springDataRepository.findAllByInventarioIdOrderByCreadoEnDesc(inventoryId)
                .stream()
                .map(MovimientoStockEntity::toDomain)
                .toList();
    }

    @Override
    public List<MovimientoStock> findByBuffetAndDateRange(
            UUID buffetId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return springDataRepository.findByBuffetAndDateRange(buffetId, start, end)
                .stream()
                .map(MovimientoStockEntity::toDomain)
                .toList();
    }

    private InventarioBuffetEntity inventoryReference(UUID inventoryId) {
        InventarioBuffetEntity inventory = new InventarioBuffetEntity();
        inventory.setId(inventoryId);
        return inventory;
    }

    private UsuarioEntity userReference(UUID userId) {
        if (userId == null) {
            return null;
        }

        UsuarioEntity user = new UsuarioEntity();
        user.setId(userId);
        return user;
    }
}
