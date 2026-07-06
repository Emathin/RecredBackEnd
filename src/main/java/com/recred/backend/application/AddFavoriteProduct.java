package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class AddFavoriteProduct {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final TutorRepository tutorRepository;

    public AddFavoriteProduct(AlumnoRepository alumnoRepository,
                              ProductRepository productRepository,
                              TutorRepository tutorRepository) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public AddFavoriteProductResponse execute(AddFavoriteProductRequest addFavoriteRequest) {
        Alumno student = findAlumnoById(addFavoriteRequest.alumnoId());
        Producto product = findProductById(addFavoriteRequest.productoId());

        ensureFavoriteProductLimitIsNotExceeded(student, addFavoriteRequest);
        addFavoriteProduct(student, product);

        return new AddFavoriteProductResponse();
    }

    private void ensureFavoriteProductLimitIsNotExceeded(
            Alumno student,
            AddFavoriteProductRequest addFavoriteRequest) {
        if (!hasOnlyFreeTutors(addFavoriteRequest.alumnoId())) {
            return;
        }

        if (student.exceedsFavoriteProductLimit(addFavoriteRequest.productoId(), 5)) {
            throw new IllegalArgumentException(
                    "Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)");
        }
    }

    private Alumno findAlumnoById(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private Producto findProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    private void addFavoriteProduct(Alumno student, Producto product) {
        student.agregarProductoFavorito(product);
        alumnoRepository.save(student);
    }

    private boolean hasOnlyFreeTutors(UUID studentId) {
        List<Tutor> tutores = tutorRepository.findByAlumnoId(studentId);
        return tutores.isEmpty() || tutores.stream()
                .allMatch(tutor -> tutor.getUsuario() == null
                        || tutor.getUsuario().hasFreePlan());
    }
}
