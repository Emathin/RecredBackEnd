package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataBuffetRepository extends JpaRepository<BuffetEntity, UUID> {
}
