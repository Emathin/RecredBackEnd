package com.recred.backend.application;

import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.ClasificacionSalud;
import com.recred.backend.dominio.repositories.AlumnoRepository;
import com.recred.backend.dominio.repositories.ClasificacionSaludRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdministrarRestricciones {

    private final AlumnoRepository alumnoRepository;
    private final ClasificacionSaludRepository clasificacionRepository;

    public AdministrarRestricciones(AlumnoRepository alumnoRepository,
                                    ClasificacionSaludRepository clasificacionRepository) {
        this.alumnoRepository = alumnoRepository;
        this.clasificacionRepository = clasificacionRepository;
    }

    public void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds) {

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId +
                        " no existe."));

        Set<ClasificacionSalud> nuevasRestricciones = new HashSet<>(
                clasificacionRepository.findAllById(nuevasClasificacionesIds)
        );

        alumno.setRestriccionesSalud(nuevasRestricciones);

        alumnoRepository.save(alumno);
    }
}