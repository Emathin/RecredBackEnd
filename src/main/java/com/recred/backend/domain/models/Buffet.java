package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Buffet {

    private UUID id;

    private Colegio colegio;
    private Kiosquero vendedor;

    private String nombre;
    private LocalDate fechaVencimientoHabilitacion;
    private Boolean activo = true;

    private Boolean habilitarVentasAnticipadasNoLaborables = false;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}