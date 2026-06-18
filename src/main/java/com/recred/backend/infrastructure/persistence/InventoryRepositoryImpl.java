package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InventoryRepositoryImpl implements InventoryRepository {

    private final SpringDataInventoryRepository springDataInventoryRepository;

    public InventoryRepositoryImpl(
            SpringDataInventoryRepository springDataInventoryRepository) {
        this.springDataInventoryRepository = springDataInventoryRepository;
    }

    @Override
    public boolean checkIfProductIsInStock(UUID productId, UUID buffetId) {
        return springDataInventoryRepository
                .existsByProductIdAndKiosqueroId(productId, buffetId);
    }

    @Override
    @Transactional
    public Inventario save(Inventario inventario) {
        InventarioBuffetEntity entity = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(
                        inventario.getBuffetId(), inventario.getProductoId())
                .orElse(new InventarioBuffetEntity());

        entity.setStockActual(safeStock(inventario.getStockActual()));
        entity.setStockReservado(safeStock(inventario.getStockReservado()));
        entity.setStockMinimo(inventario.stockMinimoEfectivo());
        entity.setEstado(safeState(inventario.estadoActual()));
        entity.setTipoManejo(safeType(inventario.getTipoManejo()));
        entity.setCupoMaximoDiario(inventario.getCupoMaximoDiario());

        if (entity.getBuffetEntity() == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setId(inventario.getBuffetId());
            entity.setBuffetEntity(buffet);
        }

        if (entity.getProductoEntity() == null) {
            ProductoEntity producto = new ProductoEntity();
            producto.setId(inventario.getProductoId());
            entity.setProductoEntity(producto);
        }

        return springDataInventoryRepository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Inventario> findByBuffetAndProduct(UUID buffetId, UUID productoId) {
        return springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(buffetId, productoId)
                .map(InventarioBuffetEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<Inventario> findLockedByBuffetAndProduct(UUID buffetId, UUID productoId) {
        return springDataInventoryRepository
                .findLockedByBuffetEntityIdAndProductoEntityId(buffetId, productoId)
                .map(InventarioBuffetEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> findAvailableProductsByBuffet(UUID buffetId) {
        return springDataInventoryRepository.findAvailableByBuffetId(buffetId).stream()
                .map(this::toProductDomain)
                .collect(Collectors.toList());
    }

    private Producto toProductDomain(InventarioBuffetEntity inventory) {
        ProductoEntity entity = inventory.getProductoEntity();
        Inventario domainInventory = inventory.toDomain();
        Producto producto = new Producto();
        producto.setId(entity.getId());
        producto.setNombre(entity.getNombre());
        producto.setStockActual(inventory.getStockActual());
        producto.setStockReservado(inventory.getStockReservado());
        producto.setStockMinimo(domainInventory.stockMinimoEfectivo());
        producto.setStockDisponible(domainInventory.stockDisponible());
        producto.setTipoManejoInventario(inventory.getTipoManejo());
        producto.setEstadoInventario(domainInventory.estadoActual());
        producto.setCupoMaximoDiario(inventory.getCupoMaximoDiario());
        producto.setUrlImagen(inventory.getUrlImagen());

        if (entity.getCategoria() != null) {
            com.recred.backend.domain.models.CategoriaProducto category =
                    new com.recred.backend.domain.models.CategoriaProducto();
            category.setId(entity.getCategoria().getId());
            category.setDescripcion(entity.getCategoria().getDescripcion());
            producto.setCategoria(category);
        }
        return producto;
    }

    private Integer safeStock(Integer stock) {
        return stock == null ? 0 : stock;
    }

    private TipoManejoInventario safeType(TipoManejoInventario type) {
        return type == null ? TipoManejoInventario.STOCK_EXACTO : type;
    }

    private EstadoInventario safeState(EstadoInventario state) {
        return state == null ? EstadoInventario.DISPONIBLE : state;
    }

    @Override
    @Transactional
    public void updateUrlImagen(UUID buffetId, UUID productoId, String urlImagen) {
        springDataInventoryRepository.updateUrlImagen(buffetId, productoId, urlImagen);
    }
}
