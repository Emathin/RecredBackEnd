package com.recred.backend.control_parental.application.usecases;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.control_parental.application.ports.out.AlumnoPort;
import com.recred.backend.control_parental.application.ports.out.ClasificacionSaludPort;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ActualizarRestriccionesSaludUseCase {

    private final AlumnoPort alumnoPort;
    private final ClasificacionSaludPort clasificacionPort;

    public ActualizarRestriccionesSaludUseCase(
            AlumnoPort alumnoPort,
            ClasificacionSaludPort clasificacionPort) {
        this.alumnoPort = alumnoPort;
        this.clasificacionPort = clasificacionPort;
    }

    public void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds) {
        // 1. Buscamos al alumno a través del puerto
        AlumnoEntity alumno = alumnoPort.buscarPorId(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno con ID " + alumnoId + " no existe."));

        // 2. Buscamos las entidades de salud correspondientes a los IDs que envio el padre
        Set<ClasificacionSaludEntity> nuevasRestricciones =
                clasificacionPort.buscarPorIds(nuevasClasificacionesIds);

        // 3. Actualizamos restricciones del alumno y Hibernate resuelve los cambios necesarios
        alumno.setRestriccionesSalud(nuevasRestricciones);

        // 4. Guardamos los cambios
        alumnoPort.guardar(alumno);
    }
}