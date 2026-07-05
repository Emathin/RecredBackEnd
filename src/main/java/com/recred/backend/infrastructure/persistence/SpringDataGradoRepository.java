package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.GradoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataGradoRepository extends JpaRepository<GradoEntity, UUID> {

    @EntityGraph(attributePaths = {"colegioEntity", "colegioEntity.nivelEntity", "nivelEntity"})
    List<GradoEntity> findByColegioEntityIdAndActivoTrueOrderByAnioAscDivisionAsc(
            UUID colegioId);

    @EntityGraph(attributePaths = {"colegioEntity", "colegioEntity.nivelEntity", "nivelEntity"})
    List<GradoEntity> findByColegioEntityIdOrderByActivoDescAnioAscDivisionAsc(
            UUID colegioId);

    @EntityGraph(attributePaths = {"colegioEntity", "colegioEntity.nivelEntity", "nivelEntity"})
    Optional<GradoEntity> findByIdAndColegioEntityId(UUID id, UUID colegioId);

    @Query("""
            select count(grade) > 0
            from GradoEntity grade
            where grade.colegioEntity.id = :colegioId
            and grade.nivelEntity.id = :nivelId
            and grade.activo = true
            and (:excludedId is null or grade.id <> :excludedId)
            and lower(grade.anio) = lower(:anio)
            and lower(grade.division) = lower(:division)
            """)
    boolean existsActiveBySchoolAndValues(
            @Param("colegioId") UUID colegioId,
            @Param("nivelId") UUID nivelId,
            @Param("excludedId") UUID excludedId,
            @Param("anio") String anio,
            @Param("division") String division);
}
