package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.infrastructure.entities.CompraEntity;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        @Query("SELECT c FROM CompraEntity c WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.fechaHora BETWEEN :inicio AND :fin " +
                        "ORDER BY c.fechaHora DESC")
        List<CompraEntity> findAllByAlumnoIdAndFechaHoraBetween(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin);

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
                        Pageable pageable);

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
                        Pageable pageable);

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
                        Pageable pageable);

        @Query("""
                        select purchase
                        from CompraEntity purchase
                        left join RetiroProgramadoEntity pickup on pickup.compraEntity = purchase
                        where purchase.buffetEntity.id = :buffetId
                        and purchase.estado in :statuses
                        and (
                            :date is null
                            or pickup.fechaProgramada = :date
                            or (pickup.id is null and purchase.fechaHora between :start and :end)
                        )
                        and (:pickupSlotId is null or pickup.franja.id = :pickupSlotId)
                        order by purchase.fechaHora desc
                        """)
        List<CompraEntity> findKiosqueroOrders(
                        @Param("buffetId") UUID buffetId,
                        @Param("statuses") List<EstadoCompra> statuses,
                        @Param("date") LocalDate date,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("pickupSlotId") UUID pickupSlotId,
                        Pageable pageable);

        @Query("""
                        select purchase
                        from CompraEntity purchase
                        where purchase.buffetEntity.id = :buffetId
                        and purchase.fechaHora between :start and :end
                        order by purchase.fechaHora asc
                        """)
        List<CompraEntity> findByBuffetIdAndDateRange(
                        @Param("buffetId") UUID buffetId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                        select purchase
                        from CompraEntity purchase
                        left join RetiroProgramadoEntity pickup
                        on pickup.compraEntity = purchase
                        where purchase.buffetEntity.id = :buffetId
                        and (
                            (pickup.id is not null and pickup.fechaProgramada
                            between :startDate and :endDate)
                            or (pickup.id is null and purchase.fechaHora
                            between :start and :end)
                        )
                        order by purchase.fechaHora asc
                        """)
        List<CompraEntity> findByBuffetAndPickupOrCreationDateRange(
                        @Param("buffetId") UUID buffetId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("""
                        select purchase
                        from CompraAnticipadaEntity purchase
                        join purchase.retiroProgramadoEntity pickup
                        where purchase.estado in :statuses
                        and (
                            pickup.fechaProgramada < :date
                            or (pickup.fechaProgramada = :date and pickup.franja.horaFin <= :time)
                        )
                        """)
        List<CompraEntity> findExpiredAdvancePurchases(
                        @Param("date") LocalDate date,
                        @Param("time") LocalTime time,
                        @Param("statuses") List<EstadoCompra> statuses);

        @Query("""
                        select purchase
                        from CompraAnticipadaEntity purchase
                        join purchase.retiroProgramadoEntity pickup
                        where purchase.buffetEntity.id = :buffetId
                        and purchase.estado in :statuses
                        and (
                            pickup.fechaProgramada < :date
                            or (pickup.fechaProgramada = :date and pickup.franja.horaFin <= :time)
                        )
                        """)
        List<CompraEntity> findExpiredAdvancePurchasesByBuffet(
                        @Param("buffetId") UUID buffetId,
                        @Param("date") LocalDate date,
                        @Param("time") LocalTime time,
                        @Param("statuses") List<EstadoCompra> statuses);

        @Query("""
                        select count(purchase)
                        from CompraAnticipadaEntity purchase
                        join purchase.retiroProgramadoEntity pickup
                        where purchase.buffetEntity.id = :buffetId
                        and pickup.franja.id = :pickupSlotId
                        and pickup.fechaProgramada = :pickupDate
                        and purchase.estado in :statuses
                        """)
        long countScheduledAdvancePurchases(
                        @Param("buffetId") UUID buffetId,
                        @Param("pickupSlotId") UUID pickupSlotId,
                        @Param("pickupDate") LocalDate pickupDate,
                        @Param("statuses") List<EstadoCompra> statuses);

        @Query("""
                        select count(purchase) > 0
                        from CompraAnticipadaEntity purchase
                        join purchase.retiroProgramadoEntity pickup
                        where pickup.franja.id = :pickupSlotId
                        and pickup.fechaProgramada >= :fromDate
                        and purchase.estado in :statuses
                        """)
        boolean existsActiveFutureScheduledByPickupSlotId(
                        @Param("pickupSlotId") UUID pickupSlotId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("statuses") List<EstadoCompra> statuses);

        @Query("""
                        select coalesce(sum(detail.cantidad), 0)
                        from CompraAnticipadaEntity purchase
                        join purchase.retiroProgramadoEntity pickup
                        join purchase.detalles detail
                        where purchase.buffetEntity.id = :buffetId
                        and detail.productoEntity.id = :productId
                        and pickup.fechaProgramada = :pickupDate
                        and purchase.estado in :statuses
                        """)
        long sumScheduledProductQuantity(
                        @Param("buffetId") UUID buffetId,
                        @Param("productId") UUID productId,
                        @Param("pickupDate") LocalDate pickupDate,
                        @Param("statuses") List<EstadoCompra> statuses);

        @Query("SELECT COALESCE(SUM(c.montoTotal), 0) FROM CompraEntity c " +
                        "WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.estado = :estadoValido " +
                        "AND c.fechaHora BETWEEN :inicio AND :fin")
        BigDecimal sumarGastosGeneralesEnRango(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("estadoValido") EstadoCompra estadoValido,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin);

        @Query("SELECT COALESCE(SUM(c.montoTotal), 0) FROM CompraPresencialEntity c " +
                        "WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.estado = :estado " +
                        "AND c.fechaHora BETWEEN :inicio AND :fin")
        BigDecimal sumarGastosPresencialesEnRango(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("estado") EstadoCompra estado,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin);

        @Query("SELECT COALESCE(SUM(c.montoTotal), 0) FROM CompraAnticipadaEntity c " +
                        "JOIN c.retiroProgramadoEntity r " +
                        "WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.estado IN :estados " +
                        "AND r.fechaProgramada BETWEEN :inicioDate AND :finDate")
        BigDecimal sumarGastosAnticipadosEnRango(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("estados") List<EstadoCompra> estados,
                        @Param("inicioDate") LocalDate inicioDate,
                        @Param("finDate") LocalDate finDate);

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
                        @Param("fin") LocalDateTime fin);

        @Query("SELECT COALESCE(SUM(d.subtotal), 0) FROM CompraPresencialEntity c " +
                        "JOIN c.detalles d " +
                        "JOIN d.productoEntity p " +
                        "WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.estado = :estado " +
                        "AND c.fechaHora BETWEEN :inicio AND :fin " +
                        "AND p.categoria.id = :categoriaId")
        BigDecimal sumarGastosPresencialesPorCategoriaEnRango(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("estado") EstadoCompra estado,
                        @Param("categoriaId") UUID categoriaId,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin);

        @Query("SELECT COALESCE(SUM(d.subtotal), 0) FROM CompraAnticipadaEntity c " +
                        "JOIN c.retiroProgramadoEntity r " +
                        "JOIN c.detalles d " +
                        "JOIN d.productoEntity p " +
                        "WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.estado IN :estados " +
                        "AND r.fechaProgramada BETWEEN :inicioDate AND :finDate " +
                        "AND p.categoria.id = :categoriaId")
        BigDecimal sumarGastosAnticipadosPorCategoriaEnRango(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("estados") List<EstadoCompra> estados,
                        @Param("categoriaId") UUID categoriaId,
                        @Param("inicioDate") LocalDate inicioDate,
                        @Param("finDate") LocalDate finDate);

        @Query("""
                        SELECT NEW com.recred.backend.domain.models.PurchaseResume(
                                    c.alumnoEntity.id,cd.productoEntity.id,
                                    p.nombre, cd.precioUnitario, c.fechaHora, c.suggestionId)
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

        @Query("SELECT c FROM CompraEntity c WHERE c.alumnoEntity.id = :alumnoId " +
                        "AND c.estado IN :estados " +
                        "ORDER BY c.fechaHora DESC")
        List<CompraEntity> findAllByAlumnoIdAndEstadoInOrderByFechaHoraDesc(
                        @Param("alumnoId") UUID alumnoId,
                        @Param("estados") List<EstadoCompra> estados);

        @Query("SELECT COUNT(c) > 0 FROM CompraAnticipadaEntity c " +
                        "WHERE c.codigoVerificacion = :codigoVerificacion " +
                        "AND c.estado IN :estados")
        boolean existsByCodigoVerificacionAndEstadoIn(
                        @Param("codigoVerificacion") String codigoVerificacion,
                        @Param("estados") List<EstadoCompra> estados);

        long countByAlumnoEntityIdAndSuggestionIdIsNotNullAndEstadoAndFechaHoraAfter(
                        UUID alumnoId, EstadoCompra estado, LocalDateTime since);
}
