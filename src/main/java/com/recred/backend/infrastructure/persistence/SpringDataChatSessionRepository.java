package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataChatSessionRepository
        extends JpaRepository<ChatSesionEntity, UUID> {

    @Query("""
            select session
            from ChatSesionEntity session
            where session.id = :sessionId
            and session.estado = :status
            and session.usuarioEntity.alumnoEntity.id = :studentId
            """)
    Optional<ChatSesionEntity> findOpenByIdAndStudentId(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId,
            @Param("status") EstadoChatSesion status
    );
}
