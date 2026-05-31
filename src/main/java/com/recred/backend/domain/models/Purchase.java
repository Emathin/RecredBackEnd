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
    private Student student;
    private BigDecimal totalAmount;
    private PurchaseState status;
    private PaymentMethod paymentMethod;
    private LocalDateTime date;

    private Map<Product, Integer> items = new HashMap<>();

    public Purchase(Student student, Map<Product, Integer> cart, PaymentMethod paymentMethod) {
        this.student = student;
        this.items = cart;
        this.paymentMethod = paymentMethod;
        this.status = initialStatus(cart);
        this.date = LocalDateTime.now();
        this.totalAmount = calculateTotal(cart);
    }

    public void changeStatusTo(PurchaseState nextStatus) {
        if (isInvalidTransition(nextStatus)) {
            InvalidPurchaseStatusTransitionException exception =
                    new InvalidPurchaseStatusTransitionException(this.status, nextStatus);
            throw exception;
        }

        this.status = nextStatus;
    }

    public void cancel() {
        if (this.status == PurchaseState.CANCELADO) {
            throw new IllegalStateException("Purchase is already cancelled.");
        }
        this.status = PurchaseState.CANCELADO;
    }

    private BigDecimal calculateTotal(Map<Product, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> productPrice(entry.getKey())
                        .multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal productPrice(Product product) {
        return product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
    }

    private PurchaseState initialStatus(Map<Product, Integer> cart) {
        return requiresPreparation(cart) ? PurchaseState.PENDIENTE : PurchaseState.ENTREGADO;
    }

    private boolean requiresPreparation(Map<Product, Integer> cart) {
        return cart.keySet().stream().anyMatch(this::requiresPreparation);
    }

    private boolean requiresPreparation(Product product) {
        return Boolean.TRUE.equals(product.getRequiresPreparation());
    }

    private boolean isInvalidTransition(PurchaseState nextStatus) {
        return this.status == null || nextStatus == null || !isAllowedTransition(nextStatus);
    }

    private boolean isAllowedTransition(PurchaseState nextStatus) {
        return switch (this.status) {
            case PENDIENTE -> nextStatus == PurchaseState.EN_PREPARACION
                    || nextStatus == PurchaseState.CANCELADO;
            case EN_PREPARACION -> nextStatus == PurchaseState.LISTO
                    || nextStatus == PurchaseState.CANCELADO;
            case LISTO -> nextStatus == PurchaseState.ENTREGADO
                    || nextStatus == PurchaseState.CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
