package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class SchedulePickup {

    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService validacionRestriccionesService;
    // ...

    public SchedulePickup(ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService validacionRestriccionesService
            /* , ... */) {
        this.timeValidator = timeValidator;
        this.validacionRestriccionesService = validacionRestriccionesService;
    }

    public void execute(UUID studentId, Set<Producto> cart, FranjaHoraria pickupSlot,
                        LocalDate pickupDate) {
        // 1. Cargar el alumno
        // Alumno student = alumnoRepository.findById(studentId)...
        Alumno student = new Alumno(); // Simulado

        // 2. Proyectar el momento exacto del retiro
        // Usamos la hora de inicio de la franja elegida como punto de control
        LocalDateTime projectedPickupTime = LocalDateTime.of(pickupDate,
                pickupSlot.getHoraInicio());

        // 3. EL CABLEADO: Validamos contra las reglas del alumno para ese momento futuro
        timeValidator.validarCompra(student, cart, projectedPickupTime);
        validacionRestriccionesService.validarCompra(student, cart);


        // 4. Procesar reserva y pago
        // orderRepository.save(new Order(...));
        // ...
    }
}