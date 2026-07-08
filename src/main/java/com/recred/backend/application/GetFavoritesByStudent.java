package com.recred.backend.application;

import com.recred.backend.application.GetFavoritesByStudentResponse.CategoriaResponse;
import com.recred.backend.application.GetFavoritesByStudentResponse.ClasificacionResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetFavoritesByStudent {

    private final AlumnoRepository alumnoRepository;

    public GetFavoritesByStudent(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional(readOnly = true)
    public Set<GetFavoritesByStudentResponse> execute(
            GetFavoritesByStudentRequest favoritesRequest) {
        Alumno student = findStudent(favoritesRequest.alumnoId());

        return student.getProductosFavoritos().stream()
                .map(this::responseFrom)
                .collect(Collectors.toSet());
    }

    private Alumno findStudent(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private GetFavoritesByStudentResponse responseFrom(Producto product) {
        return new GetFavoritesByStudentResponse(
                product.getId(),
                product.getNombre(),
                product.getDescripcion(),
                product.getPrecio(),
                product.getPeso(),
                product.getRequierePreparacion(),
                product.getActivo(),
                product.getCreadoEn(),
                product.getActualizadoEn(),
                product.getStockActual(),
                product.getStockReservado(),
                product.getStockMinimo(),
                product.getStockDisponible(),
                product.getEstadoInventario() == null
                        ? null : product.getEstadoInventario().name(),
                product.getTipoManejoInventario() == null
                        ? null : product.getTipoManejoInventario().name(),
                product.getCupoMaximoDiario(),
                product.getCupoDisponibleDia(),
                product.getUrlImagen(),
                categoryResponseFrom(product),
                healthClassificationResponsesFrom(product),
                typeClassificationResponsesFrom(product)
        );
    }

    private CategoriaResponse categoryResponseFrom(Producto product) {
        CategoriaResponse categoryResponse = null;
        if (product.getCategoria() != null) {
            categoryResponse = new CategoriaResponse(
                    product.getCategoria().getId(),
                    product.getCategoria().getDescripcion()
            );
        }
        return categoryResponse;
    }

    private Set<ClasificacionResponse> healthClassificationResponsesFrom(
            Producto product) {
        return product.getClasificacionesSalud() == null
                ? Set.of()
                : product.getClasificacionesSalud().stream()
                        .map(cs -> new ClasificacionResponse(
                                cs.getId(), cs.getDescripcion()))
                        .collect(Collectors.toSet());
    }

    private Set<ClasificacionResponse> typeClassificationResponsesFrom(
            Producto product) {
        return product.getTipos() == null
                ? Set.of()
                : product.getTipos().stream()
                        .map(ct -> new ClasificacionResponse(
                                ct.getId(), ct.getDescripcion()))
                        .collect(Collectors.toSet());
    }
}
