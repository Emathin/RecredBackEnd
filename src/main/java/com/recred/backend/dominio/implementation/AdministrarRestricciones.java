package com.recred.backend.dominio.implementation;

import com.recred.backend.dominio.entities.AlumnoEntity;
import com.recred.backend.dominio.entities.ClasificacionSaludEntity;
import com.recred.backend.dominio.AlumnoRepository;
import com.recred.backend.dominio.ClasificacionSaludRepository;
import com.recred.backend.dominio.exceptions.RestriccionSaludException;
import com.recred.backend.presentacion.AlumnoDTO;
import com.recred.backend.presentacion.ProductoDTO;
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

    @Override
    public void validarRestriccionesDeSalud(AlumnoDTO alumnoDTO, List<ProductoDTO> productosCarrito) {
        boolean sinRestricciones = alumnoDTO.getRestriccionesSalud() == null
                || alumnoDTO.getRestriccionesSalud().isEmpty();

        if (sinRestricciones) {
            return;
        }

        for (ProductoDTO productoDTO : productosCarrito) {
            if (productoDTO.getClasificacionesSalud() != null) {

                boolean esApto = Collections.disjoint(
                        alumnoDTO.getRestriccionesSalud(),
                        productoDTO.getClasificacionesSalud()
                );

                if (!esApto) {
                    String mensaje = String.format(
                            "El producto '%s' contiene ingredientes restringidos.",
                            productoDTO.getNombre()
                    );
                    throw new RestriccionSaludException(mensaje);
                }
            }
        }
    }
}