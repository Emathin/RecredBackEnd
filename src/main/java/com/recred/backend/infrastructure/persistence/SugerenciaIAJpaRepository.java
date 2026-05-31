package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SugerenciaIAJpaRepository extends JpaRepository<AISuggestionEntity, UUID> {

    List<AISuggestionEntity> findAllByAlumnoEntityIdOrderByCreadoEnDesc(UUID alumnoId);

    List<AISuggestionEntity> findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
            UUID alumnoId, IaSuggestionType tipo);

    boolean existsByAlumnoEntityIdAndProductoEntityIdAndTipoAndCreadoEnGreaterThanEqual(
            UUID alumnoId,
            UUID productoId,
            IaSuggestionType tipo,
            LocalDateTime creadoEn
    );

    @Query("""
            select count(s) > 0
            from AISuggestionEntity s
            where s.studentEntity.id = :studentId
            and s.createdAt between :startDateTime and :endDateTime
            """)
    boolean existsAnyForStudentBetween(
            @Param("studentId") UUID studentId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            select count(s) > 0
            from AISuggestionEntity s
            where s.studentEntity.id = :studentId
            and s.type = :type
            and s.createdAt >= :since
            """)
    boolean existsForStudentAndTypeSince(
            @Param("studentId") UUID studentId,
            @Param("type") IaSuggestionType type,
            @Param("since") LocalDateTime since
    );
}
