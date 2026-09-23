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
            "hijos.gradoEntity.colegioEntity.nivelEntity",
            "hijos.billeteraEntity",
            "hijos.restriccionesSalud",
            "hijos.productosFavoritos",
            "hijos.restriccionesHorarias",
            "hijos.productosBloqueados"
    })
    List<TutorEntity> findAll();

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.gradoEntity.colegioEntity.nivelEntity",
            "hijos.billeteraEntity",
            "hijos.restriccionesSalud",
            "hijos.productosFavoritos",
            "hijos.restriccionesHorarias",
            "hijos.productosBloqueados"
    })
    Optional<TutorEntity> findById(UUID id);

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.gradoEntity.colegioEntity.nivelEntity",
            "hijos.billeteraEntity",
            "hijos.restriccionesSalud",
            "hijos.productosFavoritos",
            "hijos.restriccionesHorarias",
            "hijos.productosBloqueados"
    })
    Optional<TutorEntity> findByUsuarioId(UUID usuarioId);

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.gradoEntity.colegioEntity.nivelEntity",
            "hijos.billeteraEntity",
            "hijos.restriccionesSalud",
            "hijos.productosFavoritos",
            "hijos.restriccionesHorarias",
            "hijos.productosBloqueados"
    })
    List<TutorEntity> findByTutorPrincipalId(UUID tutorPrincipalId);

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.gradoEntity.colegioEntity.nivelEntity",
            "hijos.billeteraEntity",
            "hijos.restriccionesSalud",
            "hijos.productosFavoritos",
            "hijos.restriccionesHorarias",
            "hijos.productosBloqueados"
    })
    Optional<TutorEntity> findByUsuarioIdCognito(String idCognito);

    @EntityGraph(attributePaths = {
            "hijos",
            "usuario",
            "hijos.usuario",
            "hijos.gradoEntity",
            "hijos.gradoEntity.colegioEntity",
            "hijos.gradoEntity.colegioEntity.nivelEntity",
            "hijos.billeteraEntity",
            "hijos.restriccionesSalud",
            "hijos.productosFavoritos",
            "hijos.restriccionesHorarias",
            "hijos.productosBloqueados"
    })
    @org.springframework.data.jpa.repository.Query(
            "SELECT t FROM TutorEntity t JOIN t.hijos h WHERE h.id = :alumnoId")
    List<TutorEntity> findByAlumnoId(
            @org.springframework.data.repository.query.Param("alumnoId") UUID alumnoId);

}
