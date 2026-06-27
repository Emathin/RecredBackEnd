package com.recred.backend.application;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class GetFavoriteCartsByTutor {

    private final CarritoFavoritoRepository carritoFavoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;

    public GetFavoriteCartsByTutor(
            CarritoFavoritoRepository carritoFavoritoRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository) {
        this.carritoFavoritoRepository = carritoFavoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional(readOnly = true)
    public List<GetFavoriteCartsByTutorResponse> execute(
            GetFavoriteCartsByTutorRequest request) {
        Usuario usuario = usuarioRepository.findByIdCognito(request.cognitoSub())
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new SecurityException(
                        "No se encontró perfil de tutor para el usuario"));

        List<CarritoFavorito> domainCarts = carritoFavoritoRepository.findByTutorIdAndFilters(
                tutor.getId(), request.alumnoId(), request.productoId());

        return domainCarts.stream()
                .map(cart -> {
                    List<GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse>
                            items = cart.getItems() != null
                            ? cart.getItems().entrySet().stream()
                                    .map(entry ->
                                            new GetFavoriteCartsByTutorResponse
                                                    .CarritoFavoritoItemResponse(
                                                    entry.getKey().getId(),
                                                    entry.getKey().getNombre(),
                                                    entry.getKey().getPrecio(),
                                                    entry.getValue()
                                            ))
                                    .toList()
                            : List.of();

                    return new GetFavoriteCartsByTutorResponse(
                            cart.getId(),
                            cart.getNombre(),
                            cart.getTutor() != null ? cart.getTutor().getId() : null,
                            cart.getAlumno() != null ? cart.getAlumno().getId() : null,
                            cart.getAlumno() != null ? cart.getAlumno().getNombre() : null,
                            cart.getAlumno() != null ? cart.getAlumno().getApellido() : null,
                            items
                    );
                })
                .toList();
    }
}
