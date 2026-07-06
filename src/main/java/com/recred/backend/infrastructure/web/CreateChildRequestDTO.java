package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateChildRequestDTO(
        @Pattern(regexp = "^[^@]*$", message = "El username no puede tener formato de email")
        String username,
        @NotBlank(message = "El nombre del alumno es obligatorio")
        String nombre,
        @NotBlank(message = "El apellido del alumno es obligatorio")
        String apellido,
        @NotBlank(message = "El email del alumno es obligatorio")
        @Email(message = "El formato del email no es valido")
        String email,
        String dni,
        UUID gradoId,
        String urlFotoPerfil
) {
}
