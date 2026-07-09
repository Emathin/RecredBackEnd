package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
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

    public static Billetera initialFor(Alumno alumno) {
        Billetera billetera = new Billetera();
        billetera.setAlumno(alumno);
        billetera.setSaldoCreditos(BigDecimal.ZERO);
        billetera.setLimiteCredito(BigDecimal.ZERO);
        billetera.setSaldoDeudaInterno(BigDecimal.ZERO);
        billetera.setCreditoHabilitado(false);
        return billetera;
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.saldoCreditos.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException
                    ("Not enough balance to perform this debit operation.");
        }
        this.saldoCreditos = this.saldoCreditos.subtract(amount);
    }
    
    public void credit(BigDecimal amount) {
        this.saldoCreditos = this.saldoCreditos.add(amount);
    }

    public void enableCredit(BigDecimal amount) {
        if (Boolean.TRUE.equals(this.creditoHabilitado)) {
            throw new com.recred.backend.domain.exceptions.ActiveCreditExistsException(
                    "The student already has an active credit.");
        }
        this.credit(amount);
        this.creditoHabilitado = true;
    }

    public void payCredit(BigDecimal amount) {
        if (!Boolean.TRUE.equals(this.creditoHabilitado)) {
            throw new IllegalStateException("No active credit to pay.");
        }
        this.debit(amount);
        this.creditoHabilitado = false;
    }
}
