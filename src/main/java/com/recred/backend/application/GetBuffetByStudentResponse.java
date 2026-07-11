package com.recred.backend.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetBuffetByStudentResponse(
        UUID id,
        String nombre,
        UUID colegioId,
        UUID vendedorId,
        LocalDate fechaVencimientoHabilitacion,
        Boolean activo,
        Boolean habilitarVentasAnticipadasNoLaborables,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {}
