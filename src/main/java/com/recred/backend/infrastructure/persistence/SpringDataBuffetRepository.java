package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataBuffetRepository extends JpaRepository<BuffetEntity, UUID> {
    Optional<BuffetEntity> findFirstByColegioEntityIdAndActivoTrue(UUID colegioId);

    @Query("""
        SELECT b FROM BuffetEntity b
        JOIN FETCH b.colegioEntity
        WHERE b.id = :id
    """)
    Optional<BuffetEntity> findByIdWithSchool(@Param("id") UUID id);

    @Query("""
        SELECT b FROM BuffetEntity b
        JOIN FETCH b.colegioEntity
        JOIN AlumnoEntity a ON a.gradoEntity.colegioEntity.id = b.colegioEntity.id
        WHERE a.id = :alumnoId AND b.activo = true
    """)
    Optional<BuffetEntity> findByAlumnoId(@Param("alumnoId") UUID alumnoId);

    @Query("""
        SELECT b FROM BuffetEntity b
        LEFT JOIN FETCH b.vendedor v
        LEFT JOIN FETCH v.usuario
        WHERE b.colegioEntity.id = :colegioId
    """)
    List<BuffetEntity> findByColegioEntityId(@Param("colegioId") UUID colegioId);
}
