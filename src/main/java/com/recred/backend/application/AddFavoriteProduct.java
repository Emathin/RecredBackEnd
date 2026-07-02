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
    public void execute(AddFavoriteProductRequest request) {
        UUID alumnoId = request.alumnoId();
        UUID productoId = request.productoId();

        Alumno alumno = this.findAlumnoById(alumnoId);
        Producto producto = this.findById(productoId);
        List<Tutor> tutores = this.findTutoresByAlumnoId(alumnoId);

        boolean isFree = this.isFree(tutores);

        if (isFree) {
            this.isTutorFree(alumno, productoId);
        }

        alumno.agregarProductoFavorito(producto);
        alumnoRepository.save(alumno);
    }

    private void isTutorFree(Alumno alumno, UUID productoId) {
        boolean alreadyFavorite = alumno.getProductosFavoritos().stream()
                .anyMatch(p -> p.getId().equals(productoId));
        if (!alreadyFavorite && alumno.getProductosFavoritos().size() >= 5) {
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

    private List<Tutor> findTutoresByAlumnoId(UUID alumnoId) {
        return tutorRepository.findByAlumnoId(alumnoId);
    }

    private boolean isFree(List<Tutor> tutores) {
        return tutores.isEmpty() || tutores.stream()
                .allMatch(t -> t.getUsuario() == null
                        || t.getUsuario().getPlan() == TipoPlan.GRATUITO);
    }
}
