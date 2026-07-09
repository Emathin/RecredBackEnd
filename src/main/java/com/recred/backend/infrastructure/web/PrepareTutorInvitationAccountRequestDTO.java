package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.Pattern;

public record PrepareTutorInvitationAccountRequestDTO(
        @Pattern(
                regexp = "^[a-z0-9._-]{3,30}$",
                message = "El nombre de usuario debe tener entre 3 y 30 caracteres: "
                        + "letras, numeros, punto, guion o guion bajo"
        )
        String username
) {
}
