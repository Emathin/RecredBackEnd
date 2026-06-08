package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.TutorEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTutorRepository extends
        JpaRepository<TutorEntity, UUID> {

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.billeteraEntity"
    })
    List<TutorEntity> findAll();

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.billeteraEntity"
    })
    Optional<TutorEntity> findById(UUID id);
// ...

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.billeteraEntity"
    })
    Optional<TutorEntity> findByUsuarioId(UUID usuarioId);

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.billeteraEntity"
    })
    Optional<TutorEntity> findByUsuarioIdCognito(String idCognito);

}
