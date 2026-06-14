package com.recred.backend.application;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;

import java.util.Optional;
import java.util.UUID;

public class GetBuffetByAlumno {

    private final BuffetRepository buffetRepository;
    private final AlumnoRepository alumnoRepository;

    public GetBuffetByAlumno(BuffetRepository buffetRepository, AlumnoRepository alumnoRepository) {
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
