package com.recred.backend.control_parental.domain.validators;

import com.recred.backend.control_parental.domain.model.Alumno;
import com.recred.backend.control_parental.domain.model.Producto;
import java.util.List;

public interface CompraValidator {
    void validar(Alumno alumno, List<Producto> productosCarrito);
}