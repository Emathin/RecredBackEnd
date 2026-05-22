package com.recred.backend.control_parental.infrastructure.persistence.mappers;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.control_parental.domain.model.Alumno;
import com.recred.backend.control_parental.domain.model.ClasificacionSalud;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AlumnoMapper {

    public Alumno toDomain(AlumnoEntity entity) {
        if (entity == null) return null;

        Set<ClasificacionSalud> restriccionesDominio = entity.getRestriccionesSalud().stream()
                .map(restriccionEntity -> new ClasificacionSalud(
                        restriccionEntity.getId(),
                        restriccionEntity.getDescripcion()))
                .collect(Collectors.toSet());

        return new Alumno(entity.getId(), restriccionesDominio);
    }

    public AlumnoEntity updateEntity(Alumno domain, AlumnoEntity entityExistente) {
        if (domain == null) return null;

        Set<ClasificacionSaludEntity> entidadesSalud = domain.getRestriccionesSalud().stream()
                .map(restriccionDominio -> {
                    ClasificacionSaludEntity catEntity = new ClasificacionSaludEntity();
                    catEntity.setId(restriccionDominio.getId());
                    catEntity.setDescripcion(restriccionDominio.getDescripcion());
                    return catEntity;
                })
                .collect(Collectors.toSet());

        entityExistente.setRestriccionesSalud(entidadesSalud);
        return entityExistente;
    }
}