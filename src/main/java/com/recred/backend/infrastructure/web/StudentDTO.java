package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import java.util.UUID;

public record StudentDTO(
        UUID id,
        String nombre,
        String apellido,
        Boolean activo
) {
    public static StudentDTO fromDomain(Alumno alumno) {
        String nombre = alumno.getUsuario() != null ? alumno.getUsuario().getNombre() : "Sin nombre";
        String apellido = alumno.getUsuario() != null ? alumno.getUsuario().getApellido() : "Sin apellido";

        return new StudentDTO(
                alumno.getId(),
                nombre,
                apellido,
                alumno.getActivo()
        );
    }
}