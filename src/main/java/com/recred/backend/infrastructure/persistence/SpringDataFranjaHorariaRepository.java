package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataFranjaHorariaRepository extends
        JpaRepository<FranjaHorariaEntity, UUID>{
    List<FranjaHorariaEntity> findByColegioEntityIdAndActivoTrue(UUID colegioId);
}
