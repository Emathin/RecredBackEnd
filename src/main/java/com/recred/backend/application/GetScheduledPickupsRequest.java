package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;

import java.time.LocalDate;
import java.util.UUID;

public record GetScheduledPickupsRequest(
        UUID buffetId,
        LocalDate fecha,
        EstadoRetiro estadoRetiro,
        EstadoCompra status,
        UUID franjaId,
        String search
) {}
