package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

public class ObtenerHijosPorPadre {

    private final TutorRepository tutorRepository;

    public ObtenerHijosPorPadre(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

   @Transactional(readOnly = true)
    public Set<Alumno> execute(UUID tutorId) {
        return tutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado con ID: " + tutorId))
                .getHijos();
    }
}