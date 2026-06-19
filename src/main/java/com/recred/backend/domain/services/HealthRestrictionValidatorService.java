package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;

import java.util.Set;
import java.util.stream.Collectors;

public class HealthRestrictionValidatorService {

    public void validatePurchase(Alumno alumno, Set<Producto> productos) {
        for (Producto producto : productos) {
            Set<ClasificacionSalud> conflictos = alumno.getRestriccionesSalud().stream()
                    .filter(restriccion -> producto.getClasificaciones().contains(restriccion))
                    .collect(Collectors.toSet());

            if (!conflictos.isEmpty()) {
                String restriccionesNombres = conflictos.stream()
                        .map(ClasificacionSalud::getDescripcion)
                        .collect(Collectors.joining(", "));

                throw new RestriccionSaludException(
                        "El producto " + producto.getNombre() + " no es apto para el " +
                                "alumno debido a: " + restriccionesNombres
                );
            }
        }
    }
}
