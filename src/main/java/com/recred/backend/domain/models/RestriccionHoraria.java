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
    private ClasificacionSalud clasificacionSalud;
    private boolean activa = true;
    private UUID creadoPorUsuarioId;

    public RestriccionHoraria(UUID id, UUID alumnoId, FranjaHoraria franjaHoraria,
                              CategoriaProducto categoria, ClasificacionSalud clasificacionSalud) {
        this.id = id;
        this.alumnoId = alumnoId;
        this.franjaHoraria = franjaHoraria;
        this.categoria = categoria;
        this.clasificacionSalud = clasificacionSalud;
    }

    public boolean bloquea(Producto producto, LocalDateTime momentoCompra) {
        if (!this.activa) {
            return false;
        }

        if (!this.franjaHoraria.estaDentroDeLaFranja(momentoCompra)) {
            return false;
        }

        if (this.categoria == null && this.clasificacionSalud == null) {
            return true;
        }

        boolean coincideCategoria = false;
        if (this.categoria != null && producto.getCategoria() != null) {
            coincideCategoria = producto.getCategoria().getId().equals(this.categoria.getId());
        }

        boolean coincideClasificacion = false;
        if (this.clasificacionSalud != null && producto.getClasificaciones() != null) {
            coincideClasificacion = producto.getClasificaciones().stream()
                    .anyMatch(salud -> salud.getId().equals(this.clasificacionSalud.getId()));
        }

        return coincideCategoria || coincideClasificacion;
    }

    public boolean overlapsWith(RestriccionHoraria other) {
        if (!this.activa || !other.activa) {
            return false;
        }

        boolean sameTimeSlot = this.franjaHoraria != null && other.franjaHoraria != null &&
                               this.franjaHoraria.getId().equals(other.franjaHoraria.getId());

        if (!sameTimeSlot) {
            return false;
        }

        boolean sameCategory = this.categoria != null && other.categoria != null &&
                               this.categoria.getId().equals(other.categoria.getId());

        boolean sameClassification = this.clasificacionSalud != null &&
                other.clasificacionSalud != null &&
                this.clasificacionSalud.getId().equals(other.clasificacionSalud.getId());

        return sameCategory || sameClassification;
    }

    public void disable() {
        this.activa = false;
    }
}
