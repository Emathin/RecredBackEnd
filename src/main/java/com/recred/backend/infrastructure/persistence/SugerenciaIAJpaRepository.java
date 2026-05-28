package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SugerenciaIAJpaRepository extends JpaRepository<SugerenciaIAEntity, UUID> {

    @Query("""
            select count(s) > 0
            from SugerenciaIAEntity s
            where s.alumnoEntity.id = :studentId
            and s.creadoEn between :startDateTime and :endDateTime
            """)
    boolean existsAnyForStudentBetween(
            @Param("studentId") UUID studentId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            select count(s) > 0
            from SugerenciaIAEntity s
            where s.alumnoEntity.id = :studentId
            and s.tipo = :type
            and s.creadoEn >= :since
            """)
    boolean existsForStudentAndTypeSince(
            @Param("studentId") UUID studentId,
            @Param("type") TipoSugerenciaIA type,
            @Param("since") LocalDateTime since
    );
}
