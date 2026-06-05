package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.NotificacionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, UUID> {
    @Query("""
            select notificacion
            from NotificacionEntity notificacion
            where notificacion.usuarioEntity.id = :userId
            and (:unreadOnly = false or notificacion.leida = false)
            order by notificacion.creadoEn desc
            """)
    List<NotificacionEntity> findByUserId(
            @Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );
}
