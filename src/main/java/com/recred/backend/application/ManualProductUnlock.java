package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

public class ManualProductUnlock {

    private final AlumnoRepository alumnoRepository;

    public ManualProductUnlock(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public void execute(ManualProductUnlockRequest request) {
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        alumno.desbloquearProducto(request.productoId());
        alumnoRepository.save(alumno);
    }
}