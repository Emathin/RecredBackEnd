package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


public class DeleteFavoriteProduct {

    private final AlumnoRepository alumnoRepository;

    public DeleteFavoriteProduct(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public DeleteFavoriteProductResponse execute(DeleteFavoriteProductRequest deleteRequest) {
        Alumno student = findStudent(deleteRequest.alumnoId());
        removeFavoriteProduct(student, deleteRequest.productoId());
        saveStudent(student);
        return new DeleteFavoriteProductResponse();
    }

    private Alumno findStudent(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private void removeFavoriteProduct(Alumno student, UUID productId) {
        student.removerProductoFavorito(productId);
    }

    private void saveStudent(Alumno student) {
        alumnoRepository.save(student);
    }
}
