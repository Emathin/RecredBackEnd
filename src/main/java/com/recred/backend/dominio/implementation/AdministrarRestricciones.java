package com.recred.backend.dominio.implementation;

import com.recred.backend.infraestructura.entities.AlumnoEntity;
import com.recred.backend.infraestructura.entities.ClasificacionSaludEntity;
import com.recred.backend.repositories.AlumnoRepository;
import com.recred.backend.repositories.ClasificacionSaludRepository;
import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

@Service
public class AdministrarRestricciones implements com.recred.backend.dominio.AdministrarRestricciones {

    private final AlumnoRepository alumnoRepository;
    private final ClasificacionSaludRepository clasificacionPort;

    public AdministrarRestricciones(AlumnoRepository alumnoRepository, ClasificacionSaludRepository clasificacionPort) {
        this.alumnoRepository = alumnoRepository;
        this.clasificacionPort = clasificacionPort;
    }

    public void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds) {
        AlumnoEntity alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId + " no existe."));

        Set<ClasificacionSaludEntity> nuevasRestricciones = new HashSet<>(clasificacionPort.findAllById(nuevasClasificacionesIds));

        alumno.setRestriccionesSalud(nuevasRestricciones);

        alumnoRepository.save(alumno);
    }

    @Override
    public Set<ClasificacionSaludEntity> obtener(UUID alumnoId) {
        AlumnoEntity alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId + " no existe."));

        Set<ClasificacionSaludEntity> restricciones = alumno.getRestriccionesSalud();
        if (restricciones == null || restricciones.isEmpty()) {
            return Collections.emptySet();
        }

        return restricciones;
    }
}