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
    
    // Simplification for the cart: Map of Product to Quantity
    private Map<Producto, Integer> items = new HashMap<>();

    public Purchase(Alumno student, Map<Producto, Integer> cart, MedioPago paymentMethod) {
        this.student = student;
        this.items = cart;
        this.paymentMethod = paymentMethod;
        this.status = EstadoCompra.COMPLETADA; // As it's immediate
        this.date = LocalDateTime.now();
        this.totalAmount = calculateTotal(cart);
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> entry.getKey().getPrecio().multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
