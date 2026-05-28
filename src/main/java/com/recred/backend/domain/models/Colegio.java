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
public class Colegio {

    private UUID id;
    private String nombre;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String telefono;
    private String email;
    private String cue;
    private Nivel nivel;
    private Boolean activo = true;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}