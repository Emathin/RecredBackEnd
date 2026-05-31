package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductMeasure {

    private UUID id;
    private Product product;
    private BigDecimal unitAmount;
    private MeasureUnit unit;
}