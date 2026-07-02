package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record SchedulePickupRequest(
        UUID studentId,
        Set<Producto> cart,
        FranjaHoraria pickupSlot,
        LocalDate pickupDate
) {}
