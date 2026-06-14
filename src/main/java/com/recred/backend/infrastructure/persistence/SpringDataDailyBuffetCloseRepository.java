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

    @Query("""
            select dailyClose
            from CierreDiarioBuffetEntity dailyClose
            where dailyClose.buffetEntity.id = :buffetId
            and (:startDate is null or dailyClose.fecha >= :startDate)
            and (:endDate is null or dailyClose.fecha <= :endDate)
            order by dailyClose.fecha desc
            """)
    List<CierreDiarioBuffetEntity> findByBuffetAndDateRange(
            @Param("buffetId") UUID buffetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
