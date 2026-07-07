package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class EnableSchoolCreditRequestDTO {
    private UUID parentId;

    @NotNull(message = "studentId cannot be null")
    private UUID studentId;

    @NotNull(message = "Credit amount is required")
    @Positive(message = "Credit amount must be greater than zero")
    private BigDecimal amount;

    @Positive(message = "installments must be greater than zero")
    private Integer installments;
}
