package com.recred.backend.domain.exceptions;

import java.time.LocalDate;

public class InvalidConsumptionDateRangeException extends RuntimeException {
    public InvalidConsumptionDateRangeException(LocalDate desde, LocalDate hasta) {
        super("El rango de fechas de consumo es invalido: " + desde + " - " + hasta);
    }
}
