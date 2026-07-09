package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import java.util.UUID;

public class ManualProductLock {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;

    public ManualProductLock(
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository
    ) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
    }

    public ManualProductLockResponse execute(ManualProductLockRequest lockProductRequest) {
        UUID studentId = lockProductRequest.alumnoId();
        UUID productId = lockProductRequest.productoId();
        Alumno student = findStudentById(studentId);
        Producto product = findProductById(productId);

        lockProductForStudent(student, product);

        return new ManualProductLockResponse();
    }

    private Alumno findStudentById(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private Producto findProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    private void lockProductForStudent(Alumno student, Producto product) {
        student.bloquearProducto(product);
        alumnoRepository.save(student);
    }
}
