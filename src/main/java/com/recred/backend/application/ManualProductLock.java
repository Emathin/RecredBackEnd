package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ManualProductLock {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;

    public ManualProductLock(AlumnoRepository alumnoRepository,
                             ProductRepository productRepository) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(ManualProductLockRequest request) {
        UUID alumnoId = request.alumnoId();
        UUID productoId = request.productoId();

        Alumno alumno = this.findAlumnoById(alumnoId);
        Producto producto = this.findProductoById(productoId);

        alumno.bloquearProducto(producto);
        alumnoRepository.save(alumno);
    }

    private Alumno findAlumnoById(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private Producto findProductoById(UUID productoId) {
        return productRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }
}