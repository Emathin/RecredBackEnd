package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CarritoFavorito {

    private UUID id;
    private String nombre;
    private Tutor tutor;
    private Alumno alumno;
    private Map<Producto, Integer> items = new HashMap<>();

    public CarritoFavorito(UUID id, String nombre, Tutor tutor, Alumno alumno,
                           Map<Producto, Integer> items) {
        this.id = id;
        this.nombre = nombre;
        this.tutor = tutor;
        this.alumno = alumno;
        this.items = items != null ? items : new HashMap<>();
    }

    public void validateOwnership(Tutor tutor) {
        if (this.tutor == null || !this.tutor.getId().equals(tutor.getId())) {
            throw new SecurityException("No autorizado para eliminar este carrito");
        }
    }
}
