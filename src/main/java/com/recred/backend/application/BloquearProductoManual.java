package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class BloquearProductoManual {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;

    public BloquearProductoManual(AlumnoRepository alumnoRepository,
                                  ProductRepository productRepository) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(UUID alumnoId, UUID productoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        Producto producto = productRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        alumno.bloquearProducto(producto);
        alumnoRepository.save(alumno);
    }
}