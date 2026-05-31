package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.AIEventEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recred.backend.domain.models.AIEventType;

public interface AIEventJpaRepository extends JpaRepository<AIEventEntity, UUID> {
    Optional<AIEventEntity> findFirstByTypeAndBuffetEntityIdOrderByCreatedAtDesc(
            AIEventType type,
            UUID buffetId
    );
}
