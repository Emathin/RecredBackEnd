package com.recred.backend.application;


import java.util.List;
import java.util.UUID;

public record GetSchoolOverviewResponse(
        UUID id,
        String nombre,
        String cue,

        List<BuffetDto> buffets
) {

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
