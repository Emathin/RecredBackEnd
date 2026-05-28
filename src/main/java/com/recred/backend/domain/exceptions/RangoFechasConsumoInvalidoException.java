package com.recred.backend.domain.exceptions;

import java.time.LocalDate;

public class RangoFechasConsumoInvalidoException extends RuntimeException {
    public RangoFechasConsumoInvalidoException(LocalDate desde, LocalDate hasta) {
        super("El rango de fechas de consumo es invalido: " + desde + " - " + hasta);
    }
}
