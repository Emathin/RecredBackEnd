package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ChatMensajeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("""
            select message
            from ChatMensajeEntity message
            where message.sesion.id = :sessionId
            order by message.fechaHora asc
            """)
    List<ChatMensajeEntity> findBySessionId(
            @Param("sessionId") UUID sessionId,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
            delete from ChatMensajeEntity message
            where message.sesion.id = :sessionId
            """)
    void deleteBySessionId(@Param("sessionId") UUID sessionId);
}
