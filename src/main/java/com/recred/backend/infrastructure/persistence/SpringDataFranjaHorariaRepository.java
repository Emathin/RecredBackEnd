package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataFranjaHorariaRepository extends
        JpaRepository<FranjaHorariaEntity, UUID>{
    List<FranjaHorariaEntity> findByColegioEntityIdAndActivoTrue(UUID colegioId);

    List<FranjaHorariaEntity> findByColegioEntityIdOrderByActivoDescHoraInicioAsc(
            UUID colegioId);

    Optional<FranjaHorariaEntity> findByIdAndColegioEntityId(UUID id, UUID colegioId);

    @Query("""
            select count(slot) > 0
            from FranjaHorariaEntity slot
            where slot.colegioEntity.id = :colegioId
            and slot.activo = true
            and (:excludedId is null or slot.id <> :excludedId)
            and slot.horaInicio < :horaFin
            and slot.horaFin > :horaInicio
            """)
    boolean existsActiveOverlap(
            @Param("colegioId") UUID colegioId,
            @Param("excludedId") UUID excludedId,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
