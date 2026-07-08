package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoLicenciaColegio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetSchoolOverviewResponse(
        UUID id,
        String nombre,
        String cue,
        LicenciaDto licencia,
        List<BuffetDto> buffets
) {
    public record LicenciaDto(
            EstadoLicenciaColegio estado,
            LocalDateTime fechaInicio,
            LocalDateTime fechaVencimiento,
            LocalDateTime fechaGraciaHasta,
            BigDecimal monto,
            String moneda
    ) {}

    public record BuffetDto(
            UUID id,
            String nombre,
            Boolean activo,
            VendedorDto vendedor
    ) {}

    public record VendedorDto(
            UUID id,
            String nombre,
            String apellido,
            String email,
            String cuit
    ) {}
}
