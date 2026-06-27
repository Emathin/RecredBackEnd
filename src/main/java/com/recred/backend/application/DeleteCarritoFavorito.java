package com.recred.backend.application;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class DeleteCarritoFavorito {

    private final CarritoFavoritoRepository carritoFavoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;

    public DeleteCarritoFavorito(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        this.carritoFavoritoRepository = carritoFavoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public void execute(DeleteCarritoFavoritoRequest request) {
        String cognitoSub = request.cognitoSub();
        UUID cartId = request.cartId();

        Usuario usuario = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new SecurityException(
                        "No se encontró perfil de tutor para el usuario"));

        CarritoFavorito carrito = carritoFavoritoRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado"));

        if (!carrito.getTutor().getId().equals(tutor.getId())) {
            throw new SecurityException("No autorizado para eliminar este carrito");
        }

        carritoFavoritoRepository.delete(cartId);
    }
}
