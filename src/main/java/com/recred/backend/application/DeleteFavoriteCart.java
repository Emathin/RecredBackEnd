package com.recred.backend.application;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class DeleteFavoriteCart {

    private final CarritoFavoritoRepository carritoFavoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;

    public DeleteFavoriteCart(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        this.carritoFavoritoRepository = carritoFavoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public DeleteFavoriteCartResponse execute(DeleteFavoriteCartRequest deleteCartRequest) {
        Tutor tutor = findAuthenticatedTutor(deleteCartRequest.cognitoSub());
        CarritoFavorito cart = findFavoriteCart(deleteCartRequest.cartId());

        deleteFavoriteCartOwnedByTutor(cart, tutor, deleteCartRequest.cartId());
        return new DeleteFavoriteCartResponse();
    }

    private Tutor findAuthenticatedTutor(String cognitoSub) {
        Usuario user = findAuthenticatedUser(cognitoSub);
        return findTutorByUserId(user.getId());
    }

    private Usuario findAuthenticatedUser(String cognitoSub) {
        return usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
    }

    private Tutor findTutorByUserId(UUID userId) {
        return tutorRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new SecurityException(
                        "No se encontró perfil de tutor para el usuario"));
    }

    private CarritoFavorito findFavoriteCart(UUID cartId) {
        return carritoFavoritoRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado"));
    }

    private void deleteFavoriteCartOwnedByTutor(CarritoFavorito cart, Tutor tutor, UUID cartId) {
        cart.validateOwnership(tutor);
        carritoFavoritoRepository.delete(cartId);
    }
}
