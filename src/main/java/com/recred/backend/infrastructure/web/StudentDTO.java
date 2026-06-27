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
        BigDecimal saldo,
        String urlFotoPerfil
) {
    public static StudentDTO fromDomain(Alumno alumno) {
        String nombre = alumno.getUsuario() != null ?
                alumno.getUsuario().getNombre() : "Sin nombre";
        String apellido = alumno.getUsuario() != null ?
                alumno.getUsuario().getApellido() : "Sin apellido";
        String urlFotoPerfil = alumno.getUsuario() != null ?
                alumno.getUsuario().getUrlFotoPerfil() : null;

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
                saldo,
                urlFotoPerfil
        );
    }

    public static StudentDTO fromStudentData(UUID id, String nombre, String apellido,
                                             Boolean activo, String gradoAnio,
                                             String gradoDivision, UUID colegioId,
                                             BigDecimal saldo, String urlFotoPerfil) {
        return new StudentDTO(
                id,
                nombre != null ? nombre : "Sin nombre",
                apellido != null ? apellido : "Sin apellido",
                activo,
                formatGrade(gradoAnio, gradoDivision),
                colegioId,
                saldo != null ? saldo : BigDecimal.ZERO,
                urlFotoPerfil
        );
    }

    private static String formatGrade(String anio, String division) {
        if (anio == null && division == null) {
            return null;
        }
        if (anio == null) {
            return division;
        }
        if (division == null) {
            return anio;
        }
        return anio + " " + division;
    }
}
