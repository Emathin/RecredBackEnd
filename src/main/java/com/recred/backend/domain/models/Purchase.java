package com.recred.backend.domain.models;

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
        this.status = EstadoCompra.ENTREGADO;
        this.date = LocalDateTime.now();
        this.totalAmount = calculateTotal(cart);
    }

    public void cancel() {
        if (this.status == EstadoCompra.CANCELADO) {
            throw new IllegalStateException("Purchase is already cancelled.");
        }
        this.status = EstadoCompra.CANCELADO;
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> entry.getKey().getPrecio().multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
