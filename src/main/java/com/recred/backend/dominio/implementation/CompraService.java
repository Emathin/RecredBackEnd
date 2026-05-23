package com.recred.backend.dominio.implementation;

import com.recred.backend.presentacion.AlumnoDTO;
import com.recred.backend.presentacion.ProductoDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompraService {

    private final AdministrarRestriccionesImpl administrarRestriccionesImpl;

    public CompraService(AdministrarRestriccionesImpl administrarRestriccionesImpl) {
        this.administrarRestriccionesImpl = administrarRestriccionesImpl;
    }

    public void procesarCompra(AlumnoDTO alumnoDTO, List<ProductoDTO> productosCarrito) {
        administrarRestriccionesImpl.validarRestriccionesDeSalud(alumnoDTO, productosCarrito);
    }

}