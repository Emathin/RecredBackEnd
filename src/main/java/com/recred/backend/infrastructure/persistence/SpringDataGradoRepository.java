package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.GradoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataGradoRepository extends JpaRepository<GradoEntity, UUID> {
}
