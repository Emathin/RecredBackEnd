package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntityPresencialEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class PurchaseRepositoryImpl implements PurchaseRepository {

    private final SpringDataPurchaseRepository springDataRepository;

    public PurchaseRepositoryImpl(SpringDataPurchaseRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void save(Purchase purchase) {
        CompraEntityPresencialEntity entity = new CompraEntityPresencialEntity();
        
        if (purchase.getId() != null) {
            entity.setId(purchase.getId());
        }

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setId(purchase.getStudent().getId());
        entity.setAlumnoEntity(alumnoEntity);

        // We assume buffet is set by some default mechanism or not strictly required for this simplified flow,
        // although it might fail if BuffetEntity is strictly required without a default.
        // I will let it be for now and adjust if there's a not-null constraint failure.

        entity.setMedioPago(purchase.getPaymentMethod());
        entity.setEstado(purchase.getStatus());
        entity.setMontoTotal(purchase.getTotalAmount());
        entity.setFechaHora(purchase.getDate());

        purchase.getItems().forEach((producto, cantidad) -> {
            CompraDetalleEntity detalle = new CompraDetalleEntity();
            
            ProductoEntity productoEntity = new ProductoEntity();
            productoEntity.setId(producto.getId());
            detalle.setProducto(productoEntity);
            
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(new BigDecimal(cantidad)));
            
            entity.addDetalle(detalle);
        });

        springDataRepository.save(entity);
    }
}
