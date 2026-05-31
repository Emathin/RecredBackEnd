package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Purchase {

    private UUID id;
    private Alumno student;
    private BigDecimal totalAmount;
    private EstadoCompra status;
    private MedioPago paymentMethod;
    private LocalDateTime date;

    private Map<Producto, Integer> items = new HashMap<>();

    public Purchase(Alumno student, Map<Producto, Integer> cart, MedioPago paymentMethod) {
        this.student = student;
        this.items = cart;
        this.paymentMethod = paymentMethod;
        this.status = initialStatus(cart);
        this.date = LocalDateTime.now();
        this.totalAmount = calculateTotal(cart);
    }

    public void changeStatusTo(EstadoCompra nextStatus) {
        if (isInvalidTransition(nextStatus)) {
            InvalidPurchaseStatusTransitionException exception =
                    new InvalidPurchaseStatusTransitionException(this.status, nextStatus);
            throw exception;
        }

        this.status = nextStatus;
    }

    public void cancel() {
        if (this.status == EstadoCompra.CANCELADO) {
            throw new IllegalStateException("Purchase is already cancelled.");
        }
        this.status = EstadoCompra.CANCELADO;
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> productPrice(entry.getKey())
                        .multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal productPrice(Producto product) {
        return product.getPrecio() != null ? product.getPrecio() : BigDecimal.ZERO;
    }

    private EstadoCompra initialStatus(Map<Producto, Integer> cart) {
        return requiresPreparation(cart) ? EstadoCompra.PENDIENTE : EstadoCompra.ENTREGADO;
    }

    private boolean requiresPreparation(Map<Producto, Integer> cart) {
        return cart.keySet().stream().anyMatch(this::requiresPreparation);
    }

    private boolean requiresPreparation(Producto product) {
        return Boolean.TRUE.equals(product.getRequierePreparacion());
    }

    private boolean isInvalidTransition(EstadoCompra nextStatus) {
        return this.status == null || nextStatus == null || !isAllowedTransition(nextStatus);
    }

    private boolean isAllowedTransition(EstadoCompra nextStatus) {
        return switch (this.status) {
            case PENDIENTE -> nextStatus == EstadoCompra.EN_PREPARACION
                    || nextStatus == EstadoCompra.CANCELADO;
            case EN_PREPARACION -> nextStatus == EstadoCompra.LISTO
                    || nextStatus == EstadoCompra.CANCELADO;
            case LISTO -> nextStatus == EstadoCompra.ENTREGADO
                    || nextStatus == EstadoCompra.CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
