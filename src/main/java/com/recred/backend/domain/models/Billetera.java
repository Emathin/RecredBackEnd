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
public class Billetera {

    private UUID id;
    private Alumno alumno;
    private BigDecimal saldoCreditos = BigDecimal.ZERO;
    private BigDecimal limiteCredito = BigDecimal.ZERO;
    private BigDecimal saldoDeudaInterno = BigDecimal.ZERO;
    private Boolean creditoHabilitado = false;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public boolean hasSufficientBalance(BigDecimal amount) {

        return this.saldoCreditos.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {

        }
        this.saldoCreditos = this.saldoCreditos.subtract(amount);
    }
    
    public void credit(BigDecimal amount) {
        this.saldoCreditos = this.saldoCreditos.add(amount);
    }
}
