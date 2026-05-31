package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class School {

    private UUID id;
    private String name;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String telephone;
    private String email;
    private String cue;
    private Level level;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}