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
public class Wallet {

    private UUID id;
    private Student student;
    private BigDecimal creditBalance = BigDecimal.ZERO;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private BigDecimal innerDebtBalance = BigDecimal.ZERO;
    private Boolean enabledCredit = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.creditBalance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException
                    ("Not enough balance to perform this debit operation.");
        }
        this.creditBalance = this.creditBalance.subtract(amount);
    }
    
    public void credit(BigDecimal amount) {
        this.creditBalance = this.creditBalance.add(amount);
    }
}
