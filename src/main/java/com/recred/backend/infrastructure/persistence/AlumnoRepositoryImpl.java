package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

        if (modelo.getRestriccionesSalud() != null && !modelo.getRestriccionesSalud().isEmpty()) {
            Set<ClasificacionSaludEntity> restriccionesEntities = modelo
                    .getRestriccionesSalud().stream()
                    .map(restriccionDominio -> {
                        ClasificacionSaludEntity clasificacionEntity = new
                                ClasificacionSaludEntity();
                        clasificacionEntity.setId(restriccionDominio.getId());
                        clasificacionEntity.setDescripcion(restriccionDominio.getDescripcion());
                        clasificacionEntity.setActivo(restriccionDominio.getActivo());
                        return clasificacionEntity;
                    })
                    .collect(Collectors.toSet());

            entity.setRestriccionesSalud(restriccionesEntities);
        } else {
            if (entity.getRestriccionesSalud() != null) {
                entity.getRestriccionesSalud().clear();
            }
        }

        return entity;
    }
}