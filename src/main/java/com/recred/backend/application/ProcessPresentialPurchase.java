package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
// ... (otros imports de tus repositorios y servicios de pago)

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class ProcessPresentialPurchase {

    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;
    // private final AlumnoRepository alumnoRepository;
    // private final PaymentService paymentService;

    public ProcessPresentialPurchase(ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator
            /* , ... */) {
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
    }

    public void execute(UUID studentId, Set<Producto> cart) {
        Alumno student = new Alumno(); // Simulado

        // 2. Control de Horarios (Si falla, corta acá con un 403)
        timeValidator.validarCompra(student, cart, LocalDateTime.now());

        // 3. Control Nutricional / Salud (Si falla, corta acá con su respectivo 403)
        healthValidator.validarCompra(student, cart);

        // 4. Procesar el pago y finalizar la transacción
        // paymentService.processPayment(student, cart.getTotal());
    }
}