package com.recred.backend.dominio.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Billetera {

    private UUID id;
    private Alumno alumno;
    private BigDecimal saldoCreditos = BigDecimal.ZERO;
    private BigDecimal limiteCredito = BigDecimal.ZERO;
    private BigDecimal saldoDeudaInterno = BigDecimal.ZERO;
    private Boolean creditoHabilitado = false;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

}