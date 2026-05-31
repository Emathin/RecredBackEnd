package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ChatMessageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataChatMessageRepository
        extends JpaRepository<ChatMessageEntity, UUID> {

    @Query("""
            select message
            from ChatMessageEntity message
            where message.session.id = :sessionId
            order by message.dateAndTime desc
            """)
    List<ChatMessageEntity> findRecentBySessionId(
            @Param("sessionId") UUID sessionId,
            Pageable pageable
    );
}
