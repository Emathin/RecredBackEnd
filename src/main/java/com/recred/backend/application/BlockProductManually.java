package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class BlockProductManually {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;

    public BlockProductManually(AlumnoRepository alumnoRepository,
                                ProductRepository productRepository) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(UUID studentId, UUID productId) {
        Alumno student = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        Producto product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        student.blockProduct(product);
        alumnoRepository.save(student);
    }
}
