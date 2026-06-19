package com.recred.backend.domain.exceptions;

import java.time.LocalDate;
import java.util.UUID;

public class ProductDailyCapacityExceededException extends RuntimeException {

    public ProductDailyCapacityExceededException(
            UUID productId,
            LocalDate date,
            int capacity
    ) {
        super("El producto " + productId + " supero el cupo diario de "
                + capacity + " unidades para " + date);
    }
}
