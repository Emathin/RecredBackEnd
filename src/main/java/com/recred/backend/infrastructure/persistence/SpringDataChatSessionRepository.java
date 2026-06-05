package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            select session
            from ChatSesionEntity session
            where session.id = :sessionId
            and session.estado = :status
            and session.usuarioEntity.id = :userId
            """)
    Optional<ChatSesionEntity> findOpenByIdAndUserId(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId,
            @Param("status") EstadoChatSesion status
    );

    @Query("""
            select session
            from ChatSesionEntity session
            where session.id = :sessionId
            and session.usuarioEntity.alumnoEntity.id = :studentId
            """)
    Optional<ChatSesionEntity> findByIdAndStudentId(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId
    );

    @Query("""
            select session
            from ChatSesionEntity session
            where session.id = :sessionId
            and session.usuarioEntity.id = :userId
            """)
    Optional<ChatSesionEntity> findByIdAndUserId(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId
    );

    @Query("""
            select session
            from ChatSesionEntity session
            where session.usuarioEntity.alumnoEntity.id = :studentId
            order by session.fechaUltimaActividad desc
            """)
    List<ChatSesionEntity> findByStudentId(
            @Param("studentId") UUID studentId,
            Pageable pageable
    );

    @Query("""
            select session
            from ChatSesionEntity session
            where session.usuarioEntity.id = :userId
            order by session.fechaUltimaActividad desc
            """)
    List<ChatSesionEntity> findByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
