package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcessPresentialPurchase {

    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;
    private final ValidadorPresupuestoService budgetValidator;

    public ProcessPresentialPurchase(ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator,
                                     ValidadorPresupuestoService budgetValidator) {
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
    }

    public void execute(UUID studentId, Set<Producto> cart) {
        Alumno student = new Alumno();
        student.setId(studentId);

        LocalDateTime momentoCompra = LocalDateTime.now();

        timeValidator.validarCompra(student, cart, momentoCompra);
        healthValidator.validarCompra(student, cart);

        BigDecimal montoTotalCarrito = cart.stream()
                .map(p -> p.getPrecio() != null ?
                        p.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, BigDecimal> montosPorCategoria = cart.stream()
                .filter(p -> p.getCategoria() != null &&
                        p.getCategoria().getId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCategoria().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getPrecio() != null ?
                                        p.getPrecio() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));

        budgetValidator.validarPresupuesto(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                momentoCompra
        );

    }
}