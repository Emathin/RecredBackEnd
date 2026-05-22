package com.recred.backend.control_parental.infrastructure.persistence;

import com.recred.backend.control_parental.application.ports.out.AlumnoPort;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import com.recred.backend.usuarios.infrastructure.repositories.AlumnoRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AlumnoPersistenceAdapter implements AlumnoPort {

    private final AlumnoRepository repository;

    public AlumnoPersistenceAdapter(AlumnoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AlumnoEntity> buscarPorId(UUID id) {
        return repository.findById(id);
    }

    @Override
    public AlumnoEntity guardar(AlumnoEntity alumno) {
        return repository.save(alumno);
    }
}