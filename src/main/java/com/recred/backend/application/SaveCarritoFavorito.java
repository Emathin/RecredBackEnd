package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SaveCarritoFavorito {

    private final CarritoFavoritoRepository carritoFavoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;

    public SaveCarritoFavorito(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository) {
        this.carritoFavoritoRepository = carritoFavoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public SaveCarritoFavoritoResponse execute(SaveCarritoFavoritoRequest request) {

        Usuario usuario = usuarioRepository.findByIdCognito(request.cognitoSub())
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new SecurityException(
                        "No se encontró perfil de tutor para el usuario"));

        Alumno student = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Hijo no encontrado"));

        boolean isChildOfTutor = tutor.getHijos().stream()
                .anyMatch(h -> h.getId().equals(request.alumnoId()));
        if (!isChildOfTutor) {
            throw new SecurityException(
                    "No autorizado: el alumno seleccionado no es hijo del tutor");
        }

        CarritoFavorito carrito;
        if (request.cartId() != null) {
            carrito = carritoFavoritoRepository.findById(request.cartId())
                    .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado"));

            if (!carrito.getTutor().getId().equals(tutor.getId())) {
                throw new SecurityException("No autorizado para modificar este carrito");
            }
        } else {
            if (usuario.getPlan() == TipoPlan.GRATUITO) {
                var existingCarts = carritoFavoritoRepository
                        .findByTutorIdAndFilters(tutor.getId(), null, null);
                if (existingCarts.size() >= 3) {
                    throw new IllegalArgumentException(
                            "Límite de carritos favoritos alcanzado "
                            + "para cuenta gratuita (máximo 3)");
                }
            }
            carrito = new CarritoFavorito();
            carrito.setTutor(tutor);
        }

        carrito.setNombre(request.nombre());
        carrito.setAlumno(student);

        Map<Producto, Integer> items = new HashMap<>();
        if (request.productQuantities() != null) {
            for (var entry : request.productQuantities().entrySet()) {
                UUID prodId = entry.getKey();
                Integer quantity = entry.getValue();
                if (quantity == null || quantity <= 0) {
                    throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
                }
                Producto product = productRepository.findById(prodId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Producto no encontrado: " + prodId));
                if (!product.isActive()) {
                    throw new IllegalArgumentException(
                            "El producto no está activo: " + product.getNombre());
                }
                items.put(product, quantity);
            }
        }
        carrito.setItems(items);

        CarritoFavorito saved = carritoFavoritoRepository.save(carrito);
        return toResponse(saved);
    }

    private SaveCarritoFavoritoResponse toResponse(CarritoFavorito domain) {
        List<SaveCarritoFavoritoResponse.CarritoFavoritoItemResponse> itemResponses =
                domain.getItems() != null
                ? domain.getItems().entrySet().stream()
                        .map(entry -> new SaveCarritoFavoritoResponse.CarritoFavoritoItemResponse(
                                entry.getKey().getId(),
                                entry.getKey().getNombre(),
                                entry.getKey().getPrecio(),
                                entry.getValue()
                        ))
                        .toList()
                : List.of();

        return new SaveCarritoFavoritoResponse(
                domain.getId(),
                domain.getNombre(),
                domain.getTutor() != null ? domain.getTutor().getId() : null,
                domain.getAlumno() != null ? domain.getAlumno().getId() : null,
                domain.getAlumno() != null ? domain.getAlumno().getNombre() : null,
                domain.getAlumno() != null ? domain.getAlumno().getApellido() : null,
                itemResponses
        );
    }
}
