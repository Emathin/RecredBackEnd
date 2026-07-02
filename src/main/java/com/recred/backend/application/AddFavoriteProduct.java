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
    public AddFavoriteProductResponse execute(AddFavoriteProductRequest request) {
        Alumno alumno = findAlumnoById(request.alumnoId());
        Producto producto = findById(request.productoId());
        validateFreePlanLimit(alumno, request.productoId(), request.alumnoId());
        alumno.agregarProductoFavorito(producto);
        alumnoRepository.save(alumno);
        return new AddFavoriteProductResponse();
    }

    private void validateFreePlanLimit(Alumno alumno, UUID productoId, UUID alumnoId) {
        if (!hasOnlyFreeTutors(alumnoId)) {
            return;
        }

        if (alumno.exceedsFavoriteProductLimit(productoId, 5)) {
            throw new IllegalArgumentException(
                    "Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)");
        }
    }

    private Alumno findAlumnoById(UUID id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private Producto findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    private boolean hasOnlyFreeTutors(UUID alumnoId) {
        List<Tutor> tutores = tutorRepository.findByAlumnoId(alumnoId);
        return tutores.isEmpty() || tutores.stream()
                .allMatch(tutor -> tutor.getUsuario() == null
                        || tutor.getUsuario().hasFreePlan());
    }
}
