package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.infrastructure.entities.CompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface SpringDataPurchaseRepository extends JpaRepository<CompraEntity, UUID> {

    @Query("SELECT COALESCE(SUM(c.montoTotal), 0) FROM CompraEntity c " +
            "WHERE c.alumnoEntity.id = :alumnoId " +
            "AND c.estado = :estadoValido " +
            "AND c.fechaHora BETWEEN :inicio AND :fin")
    BigDecimal sumarGastosGeneralesEnRango(
            @Param("alumnoId") UUID alumnoId,
            @Param("estadoValido") EstadoCompra estadoValido,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT COALESCE(SUM(d.subtotal), 0) FROM CompraEntity c " +
            "JOIN c.detalles d " +
            "JOIN d.productoEntity p " +
            "WHERE c.alumnoEntity.id = :alumnoId " +
            "AND c.estado = :estadoValido " +
            "AND c.fechaHora BETWEEN :inicio AND :fin " +
            "AND p.categoria.id = :categoriaId")
    BigDecimal sumarGastosPorCategoriaEnRango(
            @Param("alumnoId") UUID alumnoId,
            @Param("estadoValido") EstadoCompra estadoValido,
            @Param("categoriaId") UUID categoriaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}