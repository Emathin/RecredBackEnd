package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public interface StudentConsumptionJpaRepository
        extends JpaRepository<CompraEntity, UUID>, StudentConsumptionRepository {

    @Override
    default List<StudentConsumptionPurchase> findPurchasesByStudentIdAndDateRange(
            UUID studentId,
            Collection<EstadoCompra> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return findPurchaseEntitiesByStudentIdAndDateRange(
                studentId,
                statuses,
                startDateTime,
                endDateTime
        ).stream()
                .map(this::toPurchase)
                .toList();
    }

    @Query("""
            select distinct purchase
            from CompraEntity purchase
            left join fetch purchase.buffetEntity buffet
            left join fetch purchase.detalles detail
            left join fetch detail.productoEntity product
            left join fetch product.categoria category
            left join fetch product.clasificacionesSalud health
            where purchase.alumnoEntity.id = :studentId
            and purchase.estado in :statuses
            and purchase.fechaHora between :startDateTime and :endDateTime
            order by purchase.fechaHora desc
            """)
    List<CompraEntity> findPurchaseEntitiesByStudentIdAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    private StudentConsumptionPurchase toPurchase(CompraEntity purchase) {
        return new StudentConsumptionPurchase(
                purchase.getId(),
                purchase.getFechaHora(),
                purchase.getEstado(),
                purchase.getMedioPago(),
                purchase.getMontoTotal(),
                purchase.getBuffetEntity().getId(),
                purchase.getBuffetEntity().getNombre(),
                products(purchase)
        );
    }

    private List<StudentConsumptionProduct> products(CompraEntity purchase) {
        return purchase.getDetalles()
                .stream()
                .sorted(Comparator.comparing(detail -> detail.getProductoEntity().getNombre()))
                .map(this::toProduct)
                .toList();
    }

    private StudentConsumptionProduct toProduct(CompraDetalleEntity detail) {
        ProductoEntity product = detail.getProductoEntity();
        return new StudentConsumptionProduct(
                product.getId(),
                product.getNombre(),
                product.getCategoria().getDescripcion(),
                detail.getCantidad(),
                detail.getPrecioUnitario(),
                detail.getSubtotal(),
                product.getClasificacionesSalud().stream()
                        .map(ClasificacionSaludEntity::getDescripcion)
                        .sorted()
                        .toList()
        );
    }
}
