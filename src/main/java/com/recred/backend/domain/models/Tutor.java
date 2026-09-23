package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    private UUID id;
    private Usuario usuario;
    private Boolean activa = true;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private String dashboardConfig;
    private Boolean esColaborador = false;
    private UUID tutorPrincipalId;
    private Set<Alumno> hijos = new HashSet<>();

    public void addHijo(Alumno alumno) {
        if (alumno != null) {
            this.hijos.add(alumno);
            alumno.getTutores().add(this);
        }
    }

    public void removeHijo(Alumno alumno) {
        if (alumno != null) {
            this.hijos.remove(alumno);
            alumno.getTutores().remove(this);
        }
    }

    public void validatePurchaseFor(UUID studentId) {
        boolean isOwnChild = this.hijos.stream()
                .anyMatch(child -> child.getId().equals(studentId));
        if (!isOwnChild) {
            throw new SecurityException(
                    "No autorizado para comprar a nombre de este alumno");
        }
    }

    public Alumno childOrThrow(UUID studentId, String message) {
        return hijos.stream()
                .filter(child -> child.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(message));
    }

    public boolean hasPaidPlan() {
        return usuario != null && usuario.hasPaidPlan();
    }

    public boolean hasChildren() {
        return hijos != null && !hijos.isEmpty();
    }

    public boolean canReceiveParentSummary() {
        return hasPaidPlan() && hasChildren();
    }

    public boolean exceedsChildrenFavoriteProductLimit(
            UUID studentId,
            UUID productId,
            int maximum
    ) {
        if (childAlreadyHasFavoriteProduct(studentId, productId)) {
            return false;
        }

        return childrenFavoriteProductCount() >= maximum;
    }

    public int childrenFavoriteProductCount() {
        return hijos.stream()
                .mapToInt(child -> child.getProductosFavoritos().size())
                .sum();
    }

    private boolean childAlreadyHasFavoriteProduct(UUID studentId, UUID productId) {
        return hijos.stream()
                .filter(child -> Objects.equals(child.getId(), studentId))
                .anyMatch(child -> child.hasFavoriteProduct(productId));
    }
}
