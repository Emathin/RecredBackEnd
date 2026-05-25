package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.AIEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIEventJpaRepository extends JpaRepository<AIEventEntity, UUID> {
}
