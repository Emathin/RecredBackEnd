package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ManualProductUnlock {

    private final AlumnoRepository alumnoRepository;

    public ManualProductUnlock(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public ManualProductUnlockResponse execute(ManualProductUnlockRequest request) {
        Alumno alumno = this.findAlumnoById(request.alumnoId());
        alumno.desbloquearProducto(request.productoId());
        this.saveAlumno(alumno);
        return new ManualProductUnlockResponse();
    }

    private Alumno findAlumnoById(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private void saveAlumno(Alumno alumno) {
        alumnoRepository.save(alumno);
    }
}
