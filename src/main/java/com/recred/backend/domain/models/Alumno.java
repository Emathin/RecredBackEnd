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
    private UUID usuarioId;
    private Usuario usuario;
    private Boolean activo = true;
    private Grado grado;
    private Set<Tutor> tutores = new HashSet<>();
    private Billetera billetera;
    private Set<ClasificacionSalud> restriccionesSalud = new HashSet<>();
    private Set<Producto> productosFavoritos = new HashSet<>();
    private Set<RestriccionHoraria> restriccionesHorarias = new HashSet<>();

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
            boolean yaExiste = this.restriccionesSalud.stream()
                    .anyMatch(r -> r.getId().equals(restriccion.getId()));

            if (!yaExiste) {
                this.restriccionesSalud.add(restriccion);
            }
        }
    }

    public void removerRestriccionSalud(UUID restriccionId) {
        this.restriccionesSalud.removeIf(r -> r.getId().equals(restriccionId));
    }

    public void agregarRestriccionHoraria(RestriccionHoraria restriccion) {
        this.restriccionesHorarias.add(restriccion);
    }

    public void removerRestriccionHoraria(RestriccionHoraria restriccion) {
        this.restriccionesHorarias.remove(restriccion);
    }

    public void darDeBaja() {
        this.activo = false;
    }
}