package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class RemoveFavoriteProduct {

    private final AlumnoRepository alumnoRepository;

    public RemoveFavoriteProduct(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public void execute(UUID alumnoId, UUID productoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        alumno.removeFavoriteProduct(productoId);
        alumnoRepository.save(alumno);
    }
}
