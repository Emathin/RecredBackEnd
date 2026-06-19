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
    public void execute(UUID studentId, UUID productId) {
        Alumno student = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        student.unblockProduct(productId);
        alumnoRepository.save(student);
    }
}
