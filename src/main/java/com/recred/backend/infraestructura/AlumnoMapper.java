package com.recred.backend.infraestructura;

import com.recred.backend.infraestructura.entities.ClasificacionSaludEntity;
import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.infraestructura.entities.AlumnoEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AlumnoMapper {



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