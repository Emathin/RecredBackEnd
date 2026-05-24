package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Alumno {

    private UUID id;
    private Usuario usuario;
    private Boolean activo = true;
    private Grado grado;
    private Set<Tutor> tutores = new HashSet<>();
    private Billetera billetera;
    private Set<ClasificacionSalud> restriccionesSalud = new HashSet<>();
    private Set<Producto> productosFavoritos = new HashSet<>();

    public Alumno(UUID id, Usuario usuario, Boolean activo, Grado grado,
                  Set<ClasificacionSalud> restriccionesSalud) {
        this.id = id;
        this.usuario = usuario;
        this.activo = activo;
        this.grado = grado;
        this.restriccionesSalud = restriccionesSalud != null ? restriccionesSalud : new HashSet<>();
    }

    public void agregarRestriccionSalud(ClasificacionSalud restriccion) {
        if (restriccion != null) {
            this.restriccionesSalud.add(restriccion);
        }
    }

    public void darDeBaja() {
        this.activo = false;
    }
}