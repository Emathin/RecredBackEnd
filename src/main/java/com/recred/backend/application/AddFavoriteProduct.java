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

    private static final int FREE_PLAN_FAVORITE_PRODUCT_LIMIT = 5;

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
        List<Tutor> tutors = tutorRepository.findByAlumnoId(addFavoriteRequest.alumnoId());
        if (hasPaidTutor(tutors)) {
            return;
        }

        if (exceedsFreePlanLimit(
                tutors,
                student,
                addFavoriteRequest.productoId(),
                addFavoriteRequest.alumnoId())) {
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

    private boolean hasPaidTutor(List<Tutor> tutors) {
        return tutors.stream()
                .anyMatch(tutor -> tutor.getUsuario() != null
                        && tutor.getUsuario().hasPaidPlan());
    }

    private boolean exceedsFreePlanLimit(
            List<Tutor> tutors,
            Alumno student,
            UUID productId,
            UUID studentId
    ) {
        if (tutors.isEmpty()) {
            return student.exceedsFavoriteProductLimit(
                    productId,
                    FREE_PLAN_FAVORITE_PRODUCT_LIMIT
            );
        }

        return tutors.stream()
                .map(this::fullTutor)
                .anyMatch(tutor -> tutor.exceedsChildrenFavoriteProductLimit(
                        studentId,
                        productId,
                        FREE_PLAN_FAVORITE_PRODUCT_LIMIT
                ));
    }

    private Tutor fullTutor(Tutor tutor) {
        if (tutor.getId() == null) {
            return tutor;
        }

        return tutorRepository.findById(tutor.getId()).orElse(tutor);
    }
}
