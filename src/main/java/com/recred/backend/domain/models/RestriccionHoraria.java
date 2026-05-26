package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RestriccionHoraria {

    private UUID id;
    private UUID alumnoId;
    private FranjaHoraria franjaHoraria;
    private CategoriaProducto categoria;
    private ClasificacionTipo clasificacion;
    private boolean activa = true;

    public RestriccionHoraria(UUID id, UUID alumnoId, FranjaHoraria franjaHoraria,
                              CategoriaProducto categoria, ClasificacionTipo clasificacion) {
        this.id = id;
        this.alumnoId = alumnoId;
        this.franjaHoraria = franjaHoraria;
        this.categoria = categoria;
        this.clasificacion = clasificacion;
    }

    public boolean bloquea(Producto producto, LocalDateTime momentoCompra) {
        if (!this.activa) {
            return false;
        }

        if (!this.franjaHoraria.estaDentroDeLaFranja(momentoCompra)) {
            return false;
        }

        if (this.categoria == null && this.clasificacion == null) {
            return true;
        }

        boolean coincideCategoria = false;
        if (this.categoria != null && producto.getCategoria() != null) {
            coincideCategoria = producto.getCategoria().getId().equals(this.categoria.getId());
        }

        boolean coincideClasificacion = false;
        if (this.clasificacion != null && producto.getTipos() != null) {
            coincideClasificacion = producto.getTipos().stream()
                    .anyMatch(tipo -> tipo.getId().equals(this.clasificacion.getId()));
        }

        return coincideCategoria || coincideClasificacion;
    }
}