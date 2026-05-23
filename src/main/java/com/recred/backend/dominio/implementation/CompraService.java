package com.recred.backend.dominio.implementation;

import com.recred.backend.presentacion.AlumnoDTO;
import com.recred.backend.presentacion.ProductoDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompraService {

    private final AdministrarRestricciones administrarRestricciones;

    public CompraService(AdministrarRestricciones administrarRestricciones) {
        this.administrarRestricciones = administrarRestricciones;
    }

    public void procesarCompra(AlumnoDTO alumnoDTO, List<ProductoDTO> productosCarrito) {
        administrarRestricciones.validarRestriccionesDeSalud(alumnoDTO, productosCarrito);
    }

}