package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


public class DeleteFavoriteProduct {

    private final AlumnoRepository alumnoRepository;

    public DeleteFavoriteProduct(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public void execute(DeleteFavoriteProductRequest request) {
        Alumno alumno = this.findAlumnoById(request.alumnoId());
        alumno.removerProductoFavorito(request.productoId());
        alumnoRepository.save(alumno);
    }

    private Alumno findAlumnoById(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }
}
