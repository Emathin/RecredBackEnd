package com.recred.backend.control_parental.application.usecases;

import com.recred.backend.control_parental.application.ports.out.AlumnoPort;
import com.recred.backend.control_parental.application.ports.out.ClasificacionSaludPort;
import com.recred.backend.control_parental.domain.model.Alumno;
import com.recred.backend.control_parental.domain.model.ClasificacionSalud;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ActualizarRestriccionesSaludUseCase {

    private final AlumnoPort alumnoPort;
    private final ClasificacionSaludPort clasificacionPort;

    public ActualizarRestriccionesSaludUseCase(AlumnoPort alumnoPort, ClasificacionSaludPort clasificacionPort) {
        this.alumnoPort = alumnoPort;
        this.clasificacionPort = clasificacionPort;
    }

    public void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds) {
        Alumno alumno = alumnoPort.buscarPorId(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId + " no existe."));

        Set<ClasificacionSalud> nuevasRestricciones = clasificacionPort.buscarPorIds(nuevasClasificacionesIds);

        alumno.actualizarRestricciones(nuevasRestricciones);

        alumnoPort.guardar(alumno);
    }
}