package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class KiosqueroPayoutConfigNotFoundException extends RuntimeException {
    public KiosqueroPayoutConfigNotFoundException(UUID kiosqueroId) {
        super("No se encontró la configuración de pagos del kiosquero con ID: " + kiosqueroId);
    }
}
