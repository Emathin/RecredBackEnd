package com.recred.backend.infrastructure.ia.repositories;

import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnoJpaRepository
        extends JpaRepository<AlumnoEntity, UUID>, AlumnoRepository {
}
