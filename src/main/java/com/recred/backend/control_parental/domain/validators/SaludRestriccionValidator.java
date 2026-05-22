package com.recred.backend.control_parental.domain.validators;

import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import com.recred.backend.catalogo.infrastructure.entities.ProductoEntity;
import com.recred.backend.control_parental.domain.exceptions.RestriccionSaludException;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class SaludRestriccionValidator implements CompraValidator {

    @Override
    public void validar(AlumnoEntity alumnoEntity, List<ProductoEntity> productosCarrito) {
        boolean sinRestricciones = alumnoEntity.getRestriccionesSalud() == null
                || alumnoEntity.getRestriccionesSalud().isEmpty();

        if (sinRestricciones) {
            return;
        }

        for (ProductoEntity productoEntity : productosCarrito) {
            if (productoEntity.getClasificacionesSalud() != null) {

                boolean esApto = Collections.disjoint(
                        alumnoEntity.getRestriccionesSalud(),
                        productoEntity.getClasificacionesSalud()
                );

                if (!esApto) {
                    String mensaje = String.format(
                            "El producto '%s' contiene ingredientes restringidos.",
                            productoEntity.getNombre()
                    );
                    throw new RestriccionSaludException(mensaje);
                }
            }
        }
    }
}