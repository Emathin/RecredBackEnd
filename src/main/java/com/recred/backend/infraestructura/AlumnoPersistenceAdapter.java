package com.recred.backend.infraestructura;

import com.recred.backend.dominio.AlumnoPort;
import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.infraestructura.entities.AlumnoEntity;
import com.recred.backend.dominio.AlumnoRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AlumnoPersistenceAdapter implements AlumnoPort {

    private final AlumnoRepository repository;
    private final AlumnoMapper mapper;

    public AlumnoPersistenceAdapter(AlumnoRepository repository, AlumnoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Alumno> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Alumno alumnoDominio) {
        AlumnoEntity entityExistente = repository.findById(alumnoDominio.getId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado en BD"));

        AlumnoEntity entityActualizada = mapper.updateEntity(alumnoDominio, entityExistente);

        repository.save(entityActualizada);
    }
}