package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Summary {
    private UUID id;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private Usuario usuario;
    private String resumen;
}
