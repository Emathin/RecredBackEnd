package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;
public interface SpringDataSuggestionAiRepository
        extends JpaRepository<SugerenciaIAEntity, UUID> {
    @Query("SELECT n FROM SugerenciaIAEntity s " +
            "JOIN NotificacionEntity n ON s.id = n.sugerenciaIaEntity.id WHERE " +
            "CAST(s.tipo AS STRING) = :tomorrow")
    List<NotificacionEntity> findByTipo(String tomorrow);


    @Query("SELECT n FROM SugerenciaIAEntity s " +
            "JOIN NotificacionEntity n ON s.id = n.sugerenciaIaEntity.id WHERE " +
            "CAST(s.tipo AS STRING) = :today " +
            "and n.alumnoEntity.id = CAST (:studentId AS java.util.UUID)" +
            "")
    NotificacionEntity findByTipoAndStudentId(String today, UUID studentId);

    @Query("SELECT DISTINCT s FROM SugerenciaIAEntity s " +
           "WHERE s.buffetEntity.id = :buffetId AND s.tipo = :tipo " +
           "ORDER BY s.creadoEn DESC")
    List<SugerenciaIAEntity>
    findDistinctByBuffetEntityIdAndTipoOrderByCreadoEnDesc(
            UUID buffetId, TipoSugerenciaIA tipo);
}
