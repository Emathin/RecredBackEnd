package com.recred.backend.application;

import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import java.util.UUID;

public record SaveKiosqueroPayoutConfigCommand(
    UUID kiosqueroId,
    String destinationCvu,
    String destinationCuit,
    String accountHolderName,
    int cantidadIntervalo,
    IntervalUnit unidadIntervalo,
    PayoutScheduleStatus estado
) {}
