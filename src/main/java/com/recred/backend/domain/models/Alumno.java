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
    private String nombre;
    private String apellido;
    private Boolean activo = true;
    private Grado grado;
    private Set<Tutor> tutores = new HashSet<>();
    private Billetera billetera;
    private Set<ClasificacionSalud> restriccionesSalud = new HashSet<>();
    private Set<Producto> productosFavoritos = new HashSet<>();
    private Set<RestriccionHoraria> restriccionesHorarias = new HashSet<>();
    private Set<Producto> productosBloqueados = new HashSet<>();
    private Integer puntosRecompensa = 0;

    public Alumno(UUID id, Usuario usuario, Boolean activo, Grado grado,
            Set<ClasificacionSalud> restriccionesSalud) {
        this.id = id;
        this.usuario = usuario;
        this.activo = activo;
        this.grado = grado;
        this.restriccionesSalud = restriccionesSalud != null ? restriccionesSalud : new HashSet<>();
    }

    public void addHealthRestriction(ClasificacionSalud restriction) {
        if (restriction != null) {
            boolean alreadyExists = this.restriccionesSalud.stream()
                    .anyMatch(r -> r.getId().equals(restriction.getId()));

            if (!alreadyExists) {
                this.restriccionesSalud.add(restriction);
            }
        }
    }

    public void removeHealthRestriction(UUID restrictionId) {
        this.restriccionesSalud.removeIf(r -> r.getId().equals(restrictionId));
    }

    public void addTimeRestriction(RestriccionHoraria restriction) {
        this.restriccionesHorarias.add(restriction);
    }

    public void removeTimeRestriction(RestriccionHoraria restriction) {
        this.restriccionesHorarias.remove(restriction);
    }

    public void deactivate() {
        this.activo = false;
    }

    public void blockProduct(Producto product) {
        if (product != null) {
            boolean alreadyExists = this.productosBloqueados.stream()
                    .anyMatch(p -> p.getId().equals(product.getId()));
            if (!alreadyExists) {
                this.productosBloqueados.add(product);
            }
        }
    }

    public void unblockProduct(UUID productId) {
        this.productosBloqueados.removeIf(p -> p.getId().equals(productId));
    }

    public void addFavoriteProduct(Producto product) {
        if (product != null) {
            boolean alreadyExists = this.productosFavoritos.stream()
                    .anyMatch(p -> p.getId().equals(product.getId()));
            if (!alreadyExists) {
                this.productosFavoritos.add(product);
            }
        }
    }

    public void removeFavoriteProduct(UUID productId) {
        this.productosFavoritos.removeIf(p -> p.getId().equals(productId));
    }

    public void addRewardPoints(int points) {
        if (this.puntosRecompensa == null) {
            this.puntosRecompensa = 0;
        }
        this.puntosRecompensa += points;
    }

    public RewardLevel calculateCurrentLevel() {
        int points = this.puntosRecompensa != null ? this.puntosRecompensa : 0;
        if (points >= 250) {
            return RewardLevel.GOLD;
        }
        if (points >= 100) {
            return RewardLevel.SILVER;
        }
        return RewardLevel.BRONZE;
    }

    public Integer getPointsToNextLevel() {
        int points = this.puntosRecompensa != null ? this.puntosRecompensa : 0;
        if (points < 100) {
            return 100 - points;
        }
        if (points < 250) {
            return 250 - points;
        }
        return 0;
    }
}
