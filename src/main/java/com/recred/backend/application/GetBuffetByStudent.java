package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;

import java.util.Optional;
import java.util.UUID;

public class GetBuffetByStudent {

    private final BuffetRepository buffetRepository;
    private final AlumnoRepository alumnoRepository;

    public GetBuffetByStudent(BuffetRepository buffetRepository,
                               AlumnoRepository alumnoRepository) {
        this.buffetRepository = buffetRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public Optional<Buffet> execute(UUID alumnoId) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new AlumnoNoEncontradoException(alumnoId);
        }
        return buffetRepository.findByAlumnoId(alumnoId);
    }
}
