package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateStudentFromParentResponse(
        UUID id,
        String nombre,
        String apellido,
        Boolean activo,
        String gradoAnio,
        String gradoDivision,
        UUID colegioId,
        BigDecimal saldo,
        String urlFotoPerfil
) {
    public static CreateStudentFromParentResponse fromDomain(Alumno alumno) {
        String nombre = alumno.getUsuario() != null
                ? alumno.getUsuario().getNombre()
                : null;
        String apellido = alumno.getUsuario() != null
                ? alumno.getUsuario().getApellido()
                : null;
        String urlFotoPerfil = alumno.getUsuario() != null
                ? alumno.getUsuario().getUrlFotoPerfil()
                : null;

        String gradoAnio = null;
        String gradoDivision = null;
        UUID colegioId = null;
        if (alumno.getGrado() != null) {
            gradoAnio = alumno.getGrado().getAnio();
            gradoDivision = alumno.getGrado().getDivision();
            if (alumno.getGrado().getColegio() != null) {
                colegioId = alumno.getGrado().getColegio().getId();
            }
        }

        BigDecimal saldo = alumno.getBilletera() != null
                ? alumno.getBilletera().getSaldoCreditos()
                : BigDecimal.ZERO;

        return new CreateStudentFromParentResponse(
                alumno.getId(),
                nombre,
                apellido,
                alumno.getActivo(),
                gradoAnio,
                gradoDivision,
                colegioId,
                saldo,
                urlFotoPerfil
        );
    }
}
