package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarFotoAlumnoResponse(
        UUID id,
        String nombre,
        String apellido,
        Boolean activo,
        String grado,
        UUID colegioId,
        BigDecimal saldo,
        String urlFotoPerfil
) {}
