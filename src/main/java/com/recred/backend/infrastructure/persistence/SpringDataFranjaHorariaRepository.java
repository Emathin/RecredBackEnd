package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataFranjaHorariaRepository extends
        JpaRepository<ScheduleEntity, UUID>{
    List<ScheduleEntity> findByColegioEntityIdAndActivoTrue(UUID colegioId);
}
