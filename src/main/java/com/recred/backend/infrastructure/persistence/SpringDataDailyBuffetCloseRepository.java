package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.CierreDiarioBuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDailyBuffetCloseRepository
        extends JpaRepository<CierreDiarioBuffetEntity, UUID> {

    Optional<CierreDiarioBuffetEntity> findByBuffetEntityIdAndFecha(
            UUID buffetId,
            LocalDate fecha);

    @Query("""
            select distinct dailyClose
            from CierreDiarioBuffetEntity dailyClose
            left join fetch dailyClose.inventarioSnapshot
            where dailyClose.buffetEntity.id = :buffetId
            and dailyClose.fecha = :fecha
            """)
    Optional<CierreDiarioBuffetEntity> findWithInventorySnapshotByBuffetAndDate(
            @Param("buffetId") UUID buffetId,
            @Param("fecha") LocalDate fecha);

    List<CierreDiarioBuffetEntity> findByBuffetEntityIdOrderByFechaDesc(UUID buffetId);

    List<CierreDiarioBuffetEntity> findByBuffetEntityIdAndFechaGreaterThanEqualOrderByFechaDesc(
            UUID buffetId,
            LocalDate startDate);

    List<CierreDiarioBuffetEntity> findByBuffetEntityIdAndFechaLessThanEqualOrderByFechaDesc(
            UUID buffetId,
            LocalDate endDate);

    List<CierreDiarioBuffetEntity> findByBuffetEntityIdAndFechaBetweenOrderByFechaDesc(
            UUID buffetId,
            LocalDate startDate,
            LocalDate endDate);
}
