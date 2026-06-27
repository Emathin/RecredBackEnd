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

public class SaveFavoriteCart {

    private final CarritoFavoritoRepository carritoFavoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;

    public SaveFavoriteCart(
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
    public SaveFavoriteCartResponse execute(SaveFavoriteCartRequest request) {
        Usuario usuario = this.findUsuario(request.cognitoSub());
        Tutor tutor = this.findTutor(usuario.getId());
        Alumno student = this.findStudent(request.alumnoId());

        this.validateStudentBelongsToTutor(tutor, request.alumnoId());

        CarritoFavorito carrito = this.resolveCarritoFavorito(request.cartId(), tutor, usuario);
        carrito.setNombre(request.nombre());
        carrito.setAlumno(student);

        Map<Producto, Integer> items = this.resolveCartItems(request.productQuantities());
        carrito.setItems(items);

        CarritoFavorito saved = carritoFavoritoRepository.save(carrito);
        return toResponse(saved);
    }

    private Usuario findUsuario(String cognitoSub) {
        return usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
    }

    private Tutor findTutor(UUID usuarioId) {
        return tutorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new SecurityException(
                        "No se encontró perfil de tutor para el usuario"));
    }

    private Alumno findStudent(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Hijo no encontrado"));
    }

    private void validateStudentBelongsToTutor(Tutor tutor, UUID studentId) {
        boolean isChildOfTutor = tutor.getHijos().stream()
                .anyMatch(h -> h.getId().equals(studentId));
        if (!isChildOfTutor) {
            throw new SecurityException(
                    "No autorizado: el alumno seleccionado no es hijo del tutor");
        }
    }

    private CarritoFavorito resolveCarritoFavorito(UUID cartId, Tutor tutor, Usuario usuario) {
        if (cartId != null) {
            CarritoFavorito carrito = carritoFavoritoRepository.findById(cartId)
                    .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado"));

            if (!carrito.getTutor().getId().equals(tutor.getId())) {
                throw new SecurityException("No autorizado para modificar este carrito");
            }
            return carrito;
        }

        this.validateCartLimitForFreePlan(tutor, usuario);

        CarritoFavorito newCarrito = new CarritoFavorito();
        newCarrito.setTutor(tutor);
        return newCarrito;
    }

    private void validateCartLimitForFreePlan(Tutor tutor, Usuario usuario) {
        if (usuario.getPlan() == TipoPlan.GRATUITO) {
            var existingCarts = carritoFavoritoRepository
                    .findByTutorIdAndFilters(tutor.getId(), null, null);
            if (existingCarts.size() >= 3) {
                throw new IllegalArgumentException(
                        "Límite de carritos favoritos alcanzado "
                        + "para cuenta gratuita (máximo 3)");
            }
        }
    }

    private Map<Producto, Integer> resolveCartItems(Map<UUID, Integer> productQuantities) {
        Map<Producto, Integer> items = new HashMap<>();
        if (productQuantities != null) {
            for (var entry : productQuantities.entrySet()) {
                UUID prodId = entry.getKey();
                Integer quantity = entry.getValue();
                
                this.validateProductQuantity(quantity);
                Producto product = this.findAndValidateActiveProduct(prodId);
                
                items.put(product, quantity);
            }
        }
        return items;
    }

    private void validateProductQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
    }

    private Producto findAndValidateActiveProduct(UUID prodId) {
        Producto product = productRepository.findById(prodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + prodId));
        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "El producto no está activo: " + product.getNombre());
        }
        return product;
    }

    private SaveFavoriteCartResponse toResponse(CarritoFavorito domain) {
        List<SaveFavoriteCartResponse.CarritoFavoritoItemResponse> itemResponses =
                domain.getItems() != null
                ? domain.getItems().entrySet().stream()
                        .map(entry -> new SaveFavoriteCartResponse.CarritoFavoritoItemResponse(
                                entry.getKey().getId(),
                                entry.getKey().getNombre(),
                                entry.getKey().getPrecio(),
                                entry.getValue()
                        ))
                        .toList()
                : List.of();

        return new SaveFavoriteCartResponse(
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
