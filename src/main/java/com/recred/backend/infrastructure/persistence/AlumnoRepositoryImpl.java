package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AlumnoRepositoryImpl implements AlumnoRepository {

    private final SpringDataAlumnoRepository springDataRepository;

    public AlumnoRepositoryImpl(SpringDataAlumnoRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Alumno> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(AlumnoEntity::toDomain);
    }

    @Override
    public void save(Alumno alumno) {
        AlumnoEntity entityExistente = springDataRepository.findById(alumno.getId())
                .orElseGet(AlumnoEntity::new);

        AlumnoEntity entityLista = toEntity(alumno, entityExistente);

        springDataRepository.save(entityLista);
    }

    private AlumnoEntity toEntity(Alumno modelo, AlumnoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setActivo(modelo.getActivo());


        return entity;
    }
}