package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class ProcessPresentialPurchase {

    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;

    public ProcessPresentialPurchase(ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator
            /* , ... */) {
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
    }

    public void execute(UUID studentId, Set<Producto> cart) {
        Alumno student = new Alumno();

        timeValidator.validarCompra(student, cart, LocalDateTime.now());

        healthValidator.validarCompra(student, cart);

    }
}