package com.recred.backend.infrastructure.persistence;

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
}
