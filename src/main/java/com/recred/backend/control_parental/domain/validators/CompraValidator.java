package com.recred.backend.control_parental.domain.validators;

import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.Producto;
import java.util.List;

public interface CompraValidator {
    void validar(Alumno alumno, List<Producto> productosCarrito);
}