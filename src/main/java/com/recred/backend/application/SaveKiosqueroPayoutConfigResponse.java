package com.recred.backend.application;

import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SaveKiosqueroPayoutConfigResponse(
    UUID kiosqueroId,
    String destinationCvu,
    String destinationCuit,
    String accountHolderName,
    int cantidadIntervalo,
    IntervalUnit unidadIntervalo,
    PayoutScheduleStatus estado,
    LocalDate proximaEjecucion
) {}
