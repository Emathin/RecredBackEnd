package com.recred.backend.application;

import com.recred.backend.dominio.AlumnoRepository;
import com.recred.backend.dominio.ClasificacionSaludRepository;
import com.recred.backend.infrastructure.entities.Alumno;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdministrarRestricciones {
    private final AlumnoRepository alumnoRepository;
    private final ClasificacionSaludRepository clasificacionRepository;

    public AdministrarRestricciones(AlumnoRepository alumnoRepository,
                                    ClasificacionSaludRepository clasificacionRepository) {
        this.alumnoRepository = alumnoRepository;
        this.clasificacionRepository = clasificacionRepository;
    }

    @Transactional
    public void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId +
                        " no existe."));

        Set<ClasificacionSaludEntity> nuevasRestricciones = new HashSet<>(clasificacionRepository.
                findAllById(nuevasClasificacionesIds));

        alumno.setRestriccionesSalud(nuevasRestricciones);

        alumnoRepository.save(alumno);
    }
}
