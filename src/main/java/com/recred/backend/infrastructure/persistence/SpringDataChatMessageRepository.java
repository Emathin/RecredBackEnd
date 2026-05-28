package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ChatMensajeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataChatMessageRepository
        extends JpaRepository<ChatMensajeEntity, UUID> {

    @Query("""
            select message
            from ChatMensajeEntity message
            where message.sesion.id = :sessionId
            order by message.fechaHora desc
            """)
    List<ChatMensajeEntity> findRecentBySessionId(
            @Param("sessionId") UUID sessionId,
            Pageable pageable
    );
}
