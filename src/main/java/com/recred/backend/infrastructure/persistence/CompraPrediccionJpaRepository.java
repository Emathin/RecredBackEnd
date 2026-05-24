package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.GastoCompra;
import com.recred.backend.domain.repositories.CompraGastoRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraPrediccionJpaRepository
        extends JpaRepository<CompraEntity, UUID>, CompraGastoRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.GastoCompra(
                c.fechaHora,
                c.montoTotal
            )
            from CompraEntity c
            where c.alumnoEntity.id = :alumnoId
            and c.estado in :estados
            and c.fechaHora between :desde and :hasta
            """)
    List<GastoCompra> buscarGastos(
            @Param("alumnoId") UUID alumnoId,
            @Param("estados") Collection<EstadoCompra> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.CategoriaConsumo(
                d.productoEntity.categoria.descripcion,
                sum(d.subtotal)
            )
            from CompraDetalleEntity d
            where d.compra.alumnoEntity.id = :alumnoId
            and d.compra.estado in :estados
            and d.compra.fechaHora between :desde and :hasta
            group by d.productoEntity.categoria.descripcion
            order by sum(d.subtotal) desc
            """)
    List<CategoriaConsumo> buscarCategoriasMasConsumidas(
            @Param("alumnoId") UUID alumnoId,
            @Param("estados") Collection<EstadoCompra> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
