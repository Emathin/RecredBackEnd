package com.recred.backend.application;

import java.util.UUID;

public record RegisterSellerRequest(
        UUID buffetId,
        String username,
        String email,
        String firstName,
        String lastName,
        String dni,
        String phone,
        String cuit
) {
}
