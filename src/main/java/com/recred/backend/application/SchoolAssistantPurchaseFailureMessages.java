package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import java.util.Map;

public class SchoolAssistantPurchaseFailureMessages {

    private static final String GENERIC_PURCHASE_FAILURE_MESSAGE =
            "No pude realizar la compra ahora. Intentalo de nuevo mas tarde.";

    private static final Map<Class<? extends RuntimeException>, String> PURCHASE_FAILURE_MESSAGES =
            Map.of(
                    InsufficientBalanceException.class,
                    "No pude realizar la compra porque no tenes saldo suficiente.",
                    PickupTimeSlotNotFoundException.class,
                    "No pude realizar la compra porque ese recreo ya no esta disponible.",
                    PresupuestoExcedidoException.class,
                    "No pude realizar la compra porque supera tu presupuesto configurado.",
                    RestriccionHorariaException.class,
                    "No pude realizar la compra porque tenes una restriccion horaria activa.",
                    RestriccionSaludException.class,
                    "No pude realizar la compra porque incluye un producto que tenes restringido."
            );

    public SchoolAssistantPurchaseFailureMessages() {
    }

    String message(RuntimeException exception) {
        return PURCHASE_FAILURE_MESSAGES.getOrDefault(
                exception.getClass(),
                GENERIC_PURCHASE_FAILURE_MESSAGE
        );
    }
}
