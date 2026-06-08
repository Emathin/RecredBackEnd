package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import java.util.Map;

class SchoolAssistantPurchaseFailureMessages {

    private static final String GENERIC_PURCHASE_FAILURE_MESSAGE =
            "No pude realizar la compra. Intentalo de nuevo mas tarde.";

    private static final Map<Class<? extends RuntimeException>, String> PURCHASE_FAILURE_MESSAGES =
            Map.of(
                    InsufficientBalanceException.class,
                    "No pude realizar la compra porque el alumno no tiene saldo suficiente.",
                    PickupTimeSlotNotFoundException.class,
                    "No pude realizar la compra porque no hay una franja horaria disponible.",
                    PresupuestoExcedidoException.class,
                    "No pude realizar la compra porque supera el presupuesto configurado.",
                    RestriccionHorariaException.class,
                    "No pude realizar la compra porque hay una restriccion horaria activa.",
                    RestriccionSaludException.class,
                    "No pude realizar la compra porque incluye un producto restringido."
            );

    String message(RuntimeException exception) {
        return PURCHASE_FAILURE_MESSAGES.getOrDefault(
                exception.getClass(),
                GENERIC_PURCHASE_FAILURE_MESSAGE
        );
    }
}
