package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.NotificationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionJpaRepository extends JpaRepository<NotificationEntity, UUID> {
}
