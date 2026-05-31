package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(UUID kiosqueroId) {
        super("No existe buffet asociado al kiosquero con id " + kiosqueroId);
    }
}
