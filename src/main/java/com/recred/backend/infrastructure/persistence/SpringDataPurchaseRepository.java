package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.infrastructure.entities.CompraEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPurchaseRepository extends JpaRepository<CompraEntity, UUID>,
        SchoolAssistantPurchaseRepository {

    @Override
    default List<SchoolAssistantPurchase> findRecentByStudentId(UUID studentId, int limit) {
        return findRecentByStudentId(studentId, PageRequest.of(0, limit));
    }

    @Query("SELECT c FROM CompraEntity c WHERE c.alumnoEntity.id = :alumnoId " +
            "ORDER BY c.fechaHora DESC")
    List<CompraEntity> findAllByAlumnoIdOrderByFechaHoraDesc(@Param("alumnoId") UUID alumnoId);

    @Query("SELECT c FROM CompraEntity c JOIN c.alumnoEntity a " +
            "JOIN a.tutores t WHERE t.id = :tutorId ORDER BY c.fechaHora DESC")
    List<CompraEntity> findAllByTutorIdOrderByFechaHoraDesc(@Param("tutorId") UUID tutorId);

    @Query("""
            select new com.recred.backend.domain.models.SchoolAssistantPurchase(
                purchase.montoTotal,
                purchase.estado,
                purchase.medioPago,
                purchase.fechaHora
            )
            from CompraEntity purchase
            where purchase.alumnoEntity.id = :studentId
            order by purchase.fechaHora desc
            """)
    List<SchoolAssistantPurchase> findRecentByStudentId(
            @Param("studentId") UUID studentId,
            Pageable pageable
    );

    @Query("SELECT c " +
            "FROM CompraEntity c " +
            "WHERE c.alumnoEntity.id = :alumnoId " +
            "ORDER BY c.fechaHora DESC")
    List<CompraEntity> findLastByAlumno(@Param("alumnoId") UUID alumnoId, Pageable pageable);

    @Query("""
            select purchase
            from CompraEntity purchase
            where purchase.buffetEntity.id = :buffetId
            order by purchase.fechaHora desc
            """)
    List<CompraEntity> findRecentByBuffetId(
            @Param("buffetId") UUID buffetId,
            Pageable pageable
    );

    @Query("""
            select purchase
            from CompraEntity purchase
            where purchase.buffetEntity.id = :buffetId
            and purchase.estado in :statuses
            order by purchase.fechaHora desc
            """)
    List<CompraEntity> findByBuffetIdAndStatuses(
            @Param("buffetId") UUID buffetId,
            @Param("statuses") List<EstadoCompra> statuses,
            Pageable pageable
    );

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

    @Query(""" 
            SELECT NEW com.recred.backend.domain.models.PurchaseResume(
                        c.alumnoEntity.id,cd.productoEntity.id,
                                    p.nombre, cd.precioUnitario, c.fechaHora) 
            FROM CompraEntity c
                        JOIN CompraDetalleEntity cd on cd.compra.id = c.id
                         join ProductoEntity p on p.id = cd.productoEntity.id 
            WHERE c.alumnoEntity.id = CAST(:alumnoEntityId AS java.util.UUID)
                        """)
    List<PurchaseResume> findByAlumno(UUID alumnoEntityId);

    @Query("""
            select new com.recred.backend.domain.models.CartProductAffinity(
                suggestedDetail.productoEntity.id,
                count(distinct purchase.id)
            )
            from CompraEntity purchase
            join purchase.detalles cartDetail
            join purchase.detalles suggestedDetail
            where purchase.alumnoEntity.id = :alumnoId
            and purchase.buffetEntity.id = :buffetId
            and purchase.estado = :status
            and cartDetail.productoEntity.id in :cartProductIds
            and suggestedDetail.productoEntity.id not in :cartProductIds
            group by suggestedDetail.productoEntity.id
            order by count(distinct purchase.id) desc
            """)
    List<CartProductAffinity> findStudentCartAffinities(
            @Param("alumnoId") UUID alumnoId,
            @Param("buffetId") UUID buffetId,
            @Param("status") EstadoCompra status,
            @Param("cartProductIds") List<UUID> cartProductIds);

    @Query("""
            select new com.recred.backend.domain.models.CartProductAffinity(
                suggestedDetail.productoEntity.id,
                count(distinct purchase.id)
            )
            from CompraEntity purchase
            join purchase.detalles cartDetail
            join purchase.detalles suggestedDetail
            where purchase.buffetEntity.id = :buffetId
            and purchase.estado = :status
            and cartDetail.productoEntity.id in :cartProductIds
            and suggestedDetail.productoEntity.id not in :cartProductIds
            group by suggestedDetail.productoEntity.id
            order by count(distinct purchase.id) desc
            """)
    List<CartProductAffinity> findBuffetCartAffinities(
            @Param("buffetId") UUID buffetId,
            @Param("status") EstadoCompra status,
            @Param("cartProductIds") List<UUID> cartProductIds);
}
