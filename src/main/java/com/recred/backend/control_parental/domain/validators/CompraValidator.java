package com.recred.backend.control_parental.domain.validators;

import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import com.recred.backend.catalogo.infrastructure.entities.ProductoEntity;
import java.util.List;

public interface CompraValidator {
    void validar(AlumnoEntity alumnoEntity, List<ProductoEntity> productosCarrito);
}