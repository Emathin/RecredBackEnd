package com.recred.backend.dominio.implementation;

import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.Producto;
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

    }
}