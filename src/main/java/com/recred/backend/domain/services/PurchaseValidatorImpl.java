package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.BuffetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class PurchaseValidatorImpl implements PurchaseValidator {

    private final ValidadorHorarioService validadorHorarioService;
    private final ValidacionRestriccionesService validacionRestriccionesService;
    private final ValidadorPresupuestoService validadorPresupuestoService;
    private final ValidadorCalendarioService validadorCalendarioService;
    private final BuffetRepository buffetRepository;

    public PurchaseValidatorImpl(
            ValidadorHorarioService validadorHorarioService,
            ValidacionRestriccionesService validacionRestriccionesService,
            ValidadorPresupuestoService validadorPresupuestoService,
            ValidadorCalendarioService validadorCalendarioService,
            BuffetRepository buffetRepository
    ) {
        this.validadorHorarioService = validadorHorarioService;
        this.validacionRestriccionesService = validacionRestriccionesService;
        this.validadorPresupuestoService = validadorPresupuestoService;
        this.validadorCalendarioService = validadorCalendarioService;
        this.buffetRepository = buffetRepository;
    }

    @Override
    public void validate(Purchase purchase) {
        LocalDate fechaValidacion = purchase.getPickupDate() != null
                ? purchase.getPickupDate()
                : LocalDate.now();

        if (purchase.getBuffetId() != null && purchase.getTipo() == TipoCompra.ANTICIPADA) {
            buffetRepository.findById(purchase.getBuffetId())
                    .ifPresent(buffet -> validadorCalendarioService
                            .validarVenta(buffet, fechaValidacion));
        }

        validadorHorarioService.validarCompra(
                purchase.getStudent(),
                purchase.getItems().keySet(),
                LocalDateTime.now()
        );

        validacionRestriccionesService.validarCompra(
                purchase.getStudent(),
                purchase.getItems().keySet()
        );

        Map<UUID, BigDecimal> amountsByCategory = purchase.getItems().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getCategoria().getId(),
                        Collectors.mapping(
                                entry -> entry.getKey().getPrecio()
                                        .multiply(BigDecimal.valueOf(entry.getValue())),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        validadorPresupuestoService.validarPresupuesto(
                purchase.getStudent().getId(),
                purchase.getTotalAmount(),
                amountsByCategory,
                LocalDateTime.now()
        );
    }
}
