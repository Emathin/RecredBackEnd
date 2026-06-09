package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataBuffetRepository extends JpaRepository<BuffetEntity, UUID> {

    @Query("""
        SELECT b FROM BuffetEntity b
        JOIN AlumnoEntity a ON a.gradoEntity.colegioEntity.id = b.colegioEntity.id
        WHERE a.id = :alumnoId AND b.activo = true
    """)
    Optional<BuffetEntity> findByAlumnoId(@Param("alumnoId") UUID alumnoId);
}

