package com.recred.backend.control_parental.application.usecases;

import com.recred.backend.control_parental.domain.ports.out.AlumnoPort;
import com.recred.backend.control_parental.domain.model.Alumno;
import com.recred.backend.control_parental.domain.model.ClasificacionSalud;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Service
public class ObtenerRestriccionesSaludUseCase {

    private final AlumnoPort alumnoPort;

    public ObtenerRestriccionesSaludUseCase(AlumnoPort alumnoPort) {
        this.alumnoPort = alumnoPort;
    }

    public Set<ClasificacionSalud> ejecutar(UUID alumnoId) {
        Alumno alumno = alumnoPort.buscarPorId(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("El alumno con ID " + alumnoId + " no existe."));

        Set<ClasificacionSalud> restricciones = alumno.getRestriccionesSalud();
        if (restricciones == null || restricciones.isEmpty()) {
            return Collections.emptySet();
        }

        return restricciones;
    }
}
