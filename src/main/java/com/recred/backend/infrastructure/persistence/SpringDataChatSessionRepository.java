package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ChatSessionState;
import com.recred.backend.infrastructure.entities.ChatSessionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataChatSessionRepository
        extends JpaRepository<ChatSessionEntity, UUID> {

    @Query("""
            select session
            from ChatSessionEntity session
            where session.id = :sessionId
            and session.state = :status
            and session.userEntity.studentEntity.id = :studentId
            """)
    Optional<ChatSessionEntity> findOpenByIdAndStudentId(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId,
            @Param("status") ChatSessionState status
    );
}
