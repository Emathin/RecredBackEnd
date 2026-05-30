package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Kiosquero {

    private UUID id;
    private Usuario usuario;
    private String cuit;
    private Buffet buffet;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

}