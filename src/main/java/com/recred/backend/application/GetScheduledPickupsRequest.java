package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoRetiro;

import java.time.LocalDate;
import java.util.UUID;

public record GetScheduledPickupsRequest(
        UUID buffetId,
        LocalDate fecha,
        EstadoRetiro estadoRetiro,
        UUID franjaId,
        String search
) {}
