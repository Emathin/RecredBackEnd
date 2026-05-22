package com.recred.backend.compras.application.services;

import com.recred.backend.control_parental.domain.model.Alumno;
import com.recred.backend.control_parental.domain.model.Producto;
import com.recred.backend.control_parental.domain.validators.CompraValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompraService {

    private final List<CompraValidator> validadores;

    public CompraService(List<CompraValidator> validadores) {
        this.validadores = validadores;
    }

    public void procesarCompra(Alumno alumno, List<Producto> productosCarrito) {

        for (CompraValidator validador : validadores) {
            validador.validar(alumno, productosCarrito);
        }

        // Si el código llega a esta línea, significa que el carrito pasó todas las pruebas.
        // TODO: Sumar total, descontar de la billetera y guardar la Compra en la base de datos.
    }
}