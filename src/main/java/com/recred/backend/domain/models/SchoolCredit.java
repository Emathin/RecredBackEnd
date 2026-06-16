package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolCredit {
    private UUID id;
    private UUID studentId;
    private BigDecimal amount;
    private CreditStatus status;
    private Integer installments;
    private LocalDateTime createdAt;
}
