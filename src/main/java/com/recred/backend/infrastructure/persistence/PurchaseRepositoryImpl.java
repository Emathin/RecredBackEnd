package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class PurchaseRepositoryImpl implements PurchaseRepository {

    private final SpringDataPurchaseRepository springDataRepository;

    public PurchaseRepositoryImpl(SpringDataPurchaseRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Purchase save(Purchase purchase) {
        CompraEntity entity = findEntityToSave(purchase);

        if (purchase.getId() != null) {
            entity.setId(purchase.getId());
        }

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setId(purchase.getStudent().getId());
        entity.setAlumnoEntity(alumnoEntity);

        entity.setMedioPago(purchase.getPaymentMethod());
        entity.setEstado(purchase.getStatus());
        entity.setMontoTotal(purchase.getTotalAmount());
        entity.setFechaHora(purchase.getDate());

        entity.getDetalles().clear();
        purchase.getItems().forEach((producto, cantidad) -> {
            CompraDetalleEntity detalle = new CompraDetalleEntity();

            ProductoEntity productoEntity = new ProductoEntity();
            productoEntity.setId(producto.getId());
            detalle.setProductoEntity(productoEntity);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(new BigDecimal(cantidad)));

            entity.addDetalle(detalle);
        });

        CompraEntity savedEntity = springDataRepository.save(entity);
        purchase.setId(savedEntity.getId());

        return savedEntity.toDomain();
    }

    private CompraEntity findEntityToSave(Purchase purchase) {
        return Optional.ofNullable(purchase.getId())
                .flatMap(springDataRepository::findById)
                .orElseGet(CompraPresencialEntity::new);
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return springDataRepository.findById(id).map(CompraEntity::toDomain);
    }

    @Override
    public BigDecimal sumarGastosPorAlumnoEnRango
            (UUID alumnoId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.sumarGastosGeneralesEnRango
                (alumnoId, EstadoCompra.ENTREGADO, inicio, fin);

    }

    @Override
    public BigDecimal sumarGastosPorCategoriaEnRango
            (UUID alumnoId, UUID categoriaId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository.sumarGastosPorCategoriaEnRango
                (alumnoId, EstadoCompra.ENTREGADO, categoriaId, inicio, fin);
    }

}
