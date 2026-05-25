package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.CompraEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataCompraRepository extends JpaRepository<CompraEntity, UUID> {
    
    @Query("SELECT c " +
            "FROM CompraEntity c " +
            "WHERE c.alumnoEntity.id = :alumnoId " +
            "ORDER BY c.fechaHora DESC")
    List<CompraEntity> findLastByAlumno(@Param("alumnoId") UUID alumnoId, Pageable pageable);
}
