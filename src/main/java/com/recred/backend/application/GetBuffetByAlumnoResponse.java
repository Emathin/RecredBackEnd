package com.recred.backend.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetBuffetByAlumnoResponse(
        UUID id,
        String nombre,
        UUID colegioId,
        UUID vendedorId,
        LocalDate fechaVencimientoHabilitacion,
        Boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {}
