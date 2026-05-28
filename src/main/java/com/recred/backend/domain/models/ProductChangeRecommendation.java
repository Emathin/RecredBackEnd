package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductChangeRecommendation {

    private UUID id;
    private UUID idBuffet;
    private String nombre;
    private BigDecimal precio;

}
