package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import java.math.BigDecimal;
import java.util.UUID;

public record StudentDTO(
        UUID id,
        String nombre,
        String apellido,
        Boolean activo,
        String grado,
        UUID colegioId,
        BigDecimal saldo
) {
    public static StudentDTO fromDomain(Alumno alumno) {
        String nombre = alumno.getUsuario() != null ?
                alumno.getUsuario().getNombre() : "Sin nombre";
        String apellido = alumno.getUsuario() != null ?
                alumno.getUsuario().getApellido() : "Sin apellido";

        String grado = null;
        UUID colegioId = null;
        if (alumno.getGrado() != null) {
            grado = alumno.getGrado().getAnio() + " " + alumno.getGrado().getDivision();
            if (alumno.getGrado().getColegio() != null) {
                colegioId = alumno.getGrado().getColegio().getId();
            }
        }

        BigDecimal saldo = alumno.getBilletera() != null ?
                alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;

        return new StudentDTO(
                alumno.getId(),
                nombre,
                apellido,
                alumno.getActivo(),
                grado,
                colegioId,
                saldo
        );
    }
}