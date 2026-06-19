package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

public class GetFavoritesByStudent {

    private final AlumnoRepository alumnoRepository;

    public GetFavoritesByStudent(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional(readOnly = true)
    public Set<Producto> execute(UUID studentId) {
        Alumno student = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        return student.getProductosFavoritos();
    }
}
