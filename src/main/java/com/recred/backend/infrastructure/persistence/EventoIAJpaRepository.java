package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.EventoIAEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoIAJpaRepository extends JpaRepository<EventoIAEntity, UUID> {
}
