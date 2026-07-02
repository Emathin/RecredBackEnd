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
    public void execute(DeleteFavoriteCartRequest request) {
        String cognitoSub = request.cognitoSub();
        UUID cartId = request.cartId();

        Usuario usuario = this.findUsuarioByCognito(cognitoSub);
        Tutor tutor = this.findTutorByUsuarioId(usuario.getId());

        CarritoFavorito carrito = this.findCarritoFavoritoById(cartId);

        analizarAutorizacionParaEliminarCarrito(tutor, carrito);

        carritoFavoritoRepository.delete(cartId);
    }

    private Usuario findUsuarioByCognito(String cognitoSub) {
        return usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
    }

    private Tutor findTutorByUsuarioId(UUID usuarioId) {
        return tutorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new SecurityException(
                        "No se encontró perfil de tutor para el usuario"));
    }

    private CarritoFavorito findCarritoFavoritoById(UUID id) {
        return carritoFavoritoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado"));
    }

    private void analizarAutorizacionParaEliminarCarrito(Tutor tutor, CarritoFavorito carrito) {
        carrito.validateOwnership(tutor);
    }
}
