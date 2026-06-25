package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class AgregarProductoFavorito {

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final TutorRepository tutorRepository;

    public AgregarProductoFavorito(AlumnoRepository alumnoRepository,
                                  ProductRepository productRepository,
                                  TutorRepository tutorRepository) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public void execute(UUID alumnoId, UUID productoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        Producto producto = productRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<Tutor> tutores = tutorRepository.findByAlumnoId(alumnoId);
        boolean isFree = tutores.isEmpty() || tutores.stream()
                .allMatch(t -> t.getUsuario() == null 
                        || t.getUsuario().getPlan() == TipoPlan.GRATUITO);

        if (isFree) {
            boolean alreadyFavorite = alumno.getProductosFavoritos().stream()
                    .anyMatch(p -> p.getId().equals(productoId));
            if (!alreadyFavorite && alumno.getProductosFavoritos().size() >= 5) {
                throw new IllegalArgumentException(
                        "Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)");
            }
        }


        alumno.agregarProductoFavorito(producto);
        alumnoRepository.save(alumno);
    }
}
