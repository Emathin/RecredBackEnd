package com.recred.backend.dominio;

import com.recred.backend.dominio.entities.ClasificacionSaludEntity;
import com.recred.backend.presentacion.AlumnoDTO;
import com.recred.backend.presentacion.ProductoDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdministrarRestricciones {
    void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds);
    Set<ClasificacionSaludEntity> obtener(UUID alumnoId);
    void validarRestriccionesDeSalud(AlumnoDTO alumnoDTO, List<ProductoDTO> productosCarrito);
}
