package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UnblockProductManually {

    private final AlumnoRepository alumnoRepository;

    public UnblockProductManually(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public void execute(UUID alumnoId, UUID productoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        alumno.desbloquearProducto(productoId);
        alumnoRepository.save(alumno);
    }
}