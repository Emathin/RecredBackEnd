package com.recred.backend.repositories;

import com.recred.backend.infraestructura.entities.AlumnoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlumnoRepository extends JpaRepository<AlumnoEntity, UUID> {
}