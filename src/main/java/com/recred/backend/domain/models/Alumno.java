package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static Alumno createFromUser(Usuario usuario, Grado grado) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario del alumno es obligatorio");
        }

        Alumno alumno = new Alumno();
        alumno.setUsuario(usuario);
        alumno.setUsuarioId(usuario.getId());
        alumno.setActivo(true);
        alumno.setGrado(grado);
        return alumno;
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

    public boolean hasGlobalHealthRestriction(UUID classificationId) {
        if (classificationId == null) {
            return false;
        }
        return this.restriccionesSalud.stream()
                .anyMatch(r -> r.getId().equals(classificationId));
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

    public void bloquearProducto(Producto producto) {
        if (producto != null) {
            boolean yaExiste = this.productosBloqueados.stream()
                    .anyMatch(p -> p.getId().equals(producto.getId()));
            if (!yaExiste) {
                this.productosBloqueados.add(producto);
            }
        }
    }

    public void desbloquearProducto(UUID productoId) {
        this.productosBloqueados.removeIf(p -> p.getId().equals(productoId));
    }

    public void agregarProductoFavorito(Producto producto) {
        if (producto != null) {
            boolean yaExiste = this.productosFavoritos.stream()
                    .anyMatch(p -> p.getId().equals(producto.getId()));
            if (!yaExiste) {
                this.productosFavoritos.add(producto);
            }
        }
    }

    public void removerProductoFavorito(UUID productoId) {
        this.productosFavoritos.removeIf(p -> p.getId().equals(productoId));
    }

    public boolean hasBlockedProduct(Producto producto) {
        if (producto == null || productosBloqueados == null) {
            return false;
        }

        return productosBloqueados.stream()
                .anyMatch(bloqueado -> Objects.equals(bloqueado.getId(), producto.getId()));
    }

    public Optional<String> healthConflictReasonFor(Producto producto) {
        if (producto == null || restriccionesSalud == null || restriccionesSalud.isEmpty()) {
            return Optional.empty();
        }

        String conflicts = restriccionesSalud.stream()
                .filter(restriccion -> hasHealthConflict(restriccion, producto))
                .map(ClasificacionSalud::getDescripcion)
                .map(this::healthConflictLabel)
                .collect(Collectors.joining(", "));

        return conflicts.isEmpty() ? Optional.empty() : Optional.of(conflicts);
    }

    public boolean hasTimeRestrictionFor(Producto producto, LocalDateTime momento) {
        if (producto == null || momento == null || restriccionesHorarias == null) {
            return false;
        }

        return restriccionesHorarias.stream()
                .anyMatch(restriccion -> restriccion.bloquea(producto, momento));
    }

    public boolean exceedsFavoriteProductLimit(UUID productoId, int maximum) {
        return !hasFavoriteProduct(productoId) && productosFavoritos.size() >= maximum;
    }

    public boolean hasFavoriteProduct(UUID productoId) {
        if (productoId == null) {
            return false;
        }

        return productosFavoritos.stream()
                .anyMatch(producto -> Objects.equals(producto.getId(), productoId));
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
            return RewardLevel.GOAT;
        }
        if (points >= 100) {
            return RewardLevel.CRACK;
        }
        return RewardLevel.PRINCIPIANTE;
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

    private boolean hasHealthConflict(ClasificacionSalud restriccion, Producto producto) {
        boolean hasClassification = producto.hasHealthClassification(restriccion);
        if (requiresFitLabel(restriccion.getDescripcion())) {
            return !hasClassification;
        }

        return hasClassification;
    }

    private boolean requiresFitLabel(String descripcion) {
        return descripcion != null
                && (descripcion.startsWith("Sin") || descripcion.contains("Vegano"));
    }

    private String healthConflictLabel(String descripcion) {
        String normalizada = normalizeDescription(descripcion);

        if ("sin tacc".equals(normalizada)) {
            return "Gluten (TACC)";
        }
        if ("sin sodio".equals(normalizada)) {
            return "Alto sodio";
        }
        if ("sin azucar".equals(normalizada)) {
            return "Azucar";
        }
        if ("apto vegano".equals(normalizada)) {
            return "Ingredientes de origen animal";
        }
        if ("contiene lacteos".equals(normalizada)) {
            return "Lacteos";
        }

        return descripcion == null ? "" : descripcion;
    }

    private String normalizeDescription(String descripcion) {
        if (descripcion == null) {
            return "";
        }

        return Normalizer.normalize(descripcion, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
