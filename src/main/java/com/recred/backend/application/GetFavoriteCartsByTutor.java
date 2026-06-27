package com.recred.backend.application;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        Usuario usuario = this.findUsuarioByCognito(request.cognitoSub());
        Tutor tutor = this.findTutorByUsuarioId(usuario.getId());

        List<CarritoFavorito> domainCarts = this.findCarritosFavoritosByTutorId(tutor.getId(),
                request.alumnoId(), request.productoId());

        return this.mapToResponses(domainCarts);
    }

    private List<GetFavoriteCartsByTutorResponse> mapToResponses(
            List<CarritoFavorito> domainCarts) {
        return domainCarts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GetFavoriteCartsByTutorResponse mapToResponse(CarritoFavorito cart) {
        List<GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse> items =
                this.mapItemsResponse(cart.getItems());

        return new GetFavoriteCartsByTutorResponse(
                cart.getId(),
                cart.getNombre(),
                cart.getTutor() != null ? cart.getTutor().getId() : null,
                cart.getAlumno() != null ? cart.getAlumno().getId() : null,
                cart.getAlumno() != null ? cart.getAlumno().getNombre() : null,
                cart.getAlumno() != null ? cart.getAlumno().getApellido() : null,
                items
        );
    }

    private List<GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse> mapItemsResponse(
            Map<Producto, Integer> itemsMap) {
        if (itemsMap == null) {
            return List.of();
        }
        return itemsMap.entrySet().stream()
                .map(this::mapItemResponse)
                .toList();
    }

    private GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse mapItemResponse(
            Map.Entry<Producto, Integer> entry) {
        return new GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse(
                entry.getKey().getId(),
                entry.getKey().getNombre(),
                entry.getKey().getPrecio(),
                entry.getValue()
        );
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

    private List<CarritoFavorito> findCarritosFavoritosByTutorId(UUID idTutor,
                                                                 UUID idAlumno, UUID idProducto) {
        return carritoFavoritoRepository.findByTutorIdAndFilters(idTutor, idAlumno, idProducto);
    }
}
