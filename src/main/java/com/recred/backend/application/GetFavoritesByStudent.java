package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

public class GetFavoritesByStudent {

    private final AlumnoRepository alumnoRepository;

    public GetFavoritesByStudent(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional(readOnly = true)
    public Set<GetFavoritesByStudentResponse> execute(GetFavoritesByStudentRequest request) {
        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        return alumno.getProductosFavoritos().stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }

    private GetFavoritesByStudentResponse toResponse(Producto producto) {
        GetFavoritesByStudentResponse.CategoriaResponse categoriaDTO = null;
        if (producto.getCategoria() != null) {
            categoriaDTO = new GetFavoritesByStudentResponse.CategoriaResponse(
                    producto.getCategoria().getId(),
                    producto.getCategoria().getDescripcion()
            );
        }

        Set<GetFavoritesByStudentResponse.ClasificacionResponse> saludDTOs =
                producto.getClasificacionesSalud() == null
                ? Set.of()
                : producto.getClasificacionesSalud().stream()
                        .map(cs -> new GetFavoritesByStudentResponse.ClasificacionResponse(
                                cs.getId(), cs.getDescripcion()))
                        .collect(Collectors.toSet());

        Set<GetFavoritesByStudentResponse.ClasificacionResponse> tiposDTOs =
                producto.getTipos() == null
                ? Set.of()
                : producto.getTipos().stream()
                        .map(ct -> new GetFavoritesByStudentResponse.ClasificacionResponse(
                                ct.getId(), ct.getDescripcion()))
                        .collect(Collectors.toSet());

        return new GetFavoritesByStudentResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getPeso(),
                producto.getRequierePreparacion(),
                producto.getActivo(),
                producto.getCreadoEn(),
                producto.getActualizadoEn(),
                producto.getStockActual(),
                producto.getStockReservado(),
                producto.getStockMinimo(),
                producto.getStockDisponible(),
                producto.getEstadoInventario() == null
                        ? null : producto.getEstadoInventario().name(),
                producto.getTipoManejoInventario() == null
                        ? null : producto.getTipoManejoInventario().name(),
                producto.getCupoMaximoDiario(),
                producto.getCupoDisponibleDia(),
                producto.getUrlImagen(),
                categoriaDTO,
                saludDTOs,
                tiposDTOs
        );
    }
}
