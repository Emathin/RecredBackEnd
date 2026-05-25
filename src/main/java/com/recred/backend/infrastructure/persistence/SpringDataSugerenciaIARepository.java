package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataSugerenciaIARepository extends JpaRepository<SugerenciaIAEntity, UUID> {
}
