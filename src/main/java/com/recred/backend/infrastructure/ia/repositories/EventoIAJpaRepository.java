package com.recred.backend.infrastructure.ia.repositories;

import com.recred.backend.infrastructure.ia.entities.EventoIAEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoIAJpaRepository extends JpaRepository<EventoIAEntity, UUID> {
}
