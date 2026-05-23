package com.recred.backend.control_parental.domain.validators;

import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.Producto;
import com.recred.backend.dominio.exceptions.RestriccionSaludException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SaludRestriccionValidator implements CompraValidator {

    @Override
    public void validar(Alumno alumno, List<Producto> productosCarrito) {
        boolean sinRestricciones = alumno.getRestriccionesSalud() == null
                || alumno.getRestriccionesSalud().isEmpty();

        if (sinRestricciones) {
            return;
        }

        for (Producto producto : productosCarrito) {
            if (producto.getClasificacionesSalud() != null) {

                boolean esApto = Collections.disjoint(
                        alumno.getRestriccionesSalud(),
                        producto.getClasificacionesSalud()
                );

                if (!esApto) {
                    String mensaje = String.format(
                            "El producto '%s' contiene ingredientes restringidos.",
                            producto.getNombre()
                    );
                    throw new RestriccionSaludException(mensaje);
                }
            }
        }
    }
}