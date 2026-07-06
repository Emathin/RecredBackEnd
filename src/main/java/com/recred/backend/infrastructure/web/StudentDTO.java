package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateStudentFromParentResponse;
import com.recred.backend.application.GetChildrenByTutorResponse;
import com.recred.backend.application.UpdateStudentPhotoResponse;
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
                alumno.getUsuario().getNombre() : null;
        String apellido = alumno.getUsuario() != null ?
                alumno.getUsuario().getApellido() : null;
        String urlFotoPerfil = alumno.getUsuario() != null ?
                alumno.getUsuario().getUrlFotoPerfil() : null;

        String grado = null;
        UUID colegioId = null;
        if (alumno.getGrado() != null) {
            grado = formatGrade(alumno.getGrado().getAnio(), alumno.getGrado().getDivision());
            if (alumno.getGrado().getColegio() != null) {
                colegioId = alumno.getGrado().getColegio().getId();
            }
        }

        BigDecimal saldo = alumno.getBilletera() != null ?
                alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;

        return fromStudentData(
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
        return fromStudentData(id, nombre, apellido, activo, formatGrade(gradoAnio, gradoDivision),
                colegioId, saldo, urlFotoPerfil);
    }

    public static StudentDTO from(GetChildrenByTutorResponse response) {
        return fromStudentData(
                response.id(),
                response.nombre(),
                response.apellido(),
                response.activo(),
                response.grado(),
                response.colegioId(),
                response.saldo(),
                response.urlFotoPerfil()
        );
    }

    public static StudentDTO from(UpdateStudentPhotoResponse response) {
        return fromStudentData(
                response.id(),
                response.nombre(),
                response.apellido(),
                response.activo(),
                response.grado(),
                response.colegioId(),
                response.saldo(),
                response.urlFotoPerfil()
        );
    }

    public static StudentDTO from(CreateStudentFromParentResponse response) {
        return fromStudentData(
                response.id(),
                response.nombre(),
                response.apellido(),
                response.activo(),
                response.gradoAnio(),
                response.gradoDivision(),
                response.colegioId(),
                response.saldo(),
                response.urlFotoPerfil()
        );
    }

    private static StudentDTO fromStudentData(UUID id, String nombre, String apellido,
                                              Boolean activo, String grado, UUID colegioId,
                                              BigDecimal saldo, String urlFotoPerfil) {
        return new StudentDTO(
                id,
                nombre != null ? nombre : "Sin nombre",
                apellido != null ? apellido : "Sin apellido",
                activo,
                grado,
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
