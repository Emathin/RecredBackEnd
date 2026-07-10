package com.recred.backend.application;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubmitSchoolRegistrationRequest(
        @NotBlank(message = "El nombre del colegio es obligatorio")
        String schoolName,
        @NotBlank(message = "El email del colegio es obligatorio")
        @Email(message = "Email inválido")
        String schoolEmail,
        @NotBlank(message = "El teléfono del colegio es obligatorio")
        String schoolPhone,
        @NotBlank(message = "El CUE del colegio es obligatorio")
        @Pattern(regexp = "^\\d{9}$", message = "CUE inválido")
        String schoolCue,
        @NotBlank(message = "El nombre del directivo es obligatorio")
        String directorFirstName,
        @NotBlank(message = "El apellido del directivo es obligatorio")
        String directorLastName,
        @NotBlank(message = "El email del directivo es obligatorio")
        @Email(message = "Email inválido")
        String directorEmail,
        @NotBlank(message = "El teléfono del directivo es obligatorio")
        String directorPhone,
        @NotBlank(message = "El DNI del directivo es obligatorio")
        @Pattern(regexp = "^\\d{7,8}$", message = "DNI inválido")
        String directorDni,
        @NotBlank(message = "El nombre de usuario del directivo es obligatorio")
        String directorUsername
) {}
