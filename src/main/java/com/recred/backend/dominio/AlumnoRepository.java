package com.recred.backend.dominio;

import com.recred.backend.dominio.entities.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlumnoRepository extends JpaRepository<Alumno, UUID> {
}