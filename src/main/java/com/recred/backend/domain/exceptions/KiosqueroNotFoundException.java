package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class KiosqueroNotFoundException extends RuntimeException {
    public KiosqueroNotFoundException(UUID kiosqueroId) {
        super("No existe buffet asociado al kiosquero con id " + kiosqueroId);
    }

    public KiosqueroNotFoundException(String cognitoSubject) {
        super("No existe kiosquero asociado al usuario autenticado " + cognitoSubject);
    }
}
