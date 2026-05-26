package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BilleteraTest {

    private Billetera billetera;

    @BeforeEach
    void setUp() {
        billetera = new Billetera();
        billetera.setId(UUID.randomUUID());
        billetera.setSaldoCreditos(new BigDecimal("100.00"));
    }

    @Test
    void hasSufficientBalance_WhenBalanceIsGreater_ShouldReturnTrue() {
        assertTrue(billetera.hasSufficientBalance(new BigDecimal("50.00")));
    }

    @Test
    void hasSufficientBalance_WhenBalanceIsEqual_ShouldReturnTrue() {
        assertTrue(billetera.hasSufficientBalance(new BigDecimal("100.00")));
    }

    @Test
    void hasSufficientBalance_WhenBalanceIsLess_ShouldReturnFalse() {
        assertFalse(billetera.hasSufficientBalance(new BigDecimal("150.00")));
    }

    @Test
    void debit_WithSufficientBalance_ShouldSubtractAmount() {
        billetera.debit(new BigDecimal("40.00"));
        assertEquals(new BigDecimal("60.00"), billetera.getSaldoCreditos());
    }

    @Test
    void debit_WithInsufficientBalance_ShouldThrowException() {
        BigDecimal amountToDebit = new BigDecimal("150.00");
        
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            billetera.debit(amountToDebit);
        });
        
        assertEquals("Not enough balance to perform this debit operation.", exception.getMessage());
        // Verify balance remains unchanged
        assertEquals(new BigDecimal("100.00"), billetera.getSaldoCreditos());
    }

    @Test
    void credit_ShouldAddAmount() {
        billetera.credit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), billetera.getSaldoCreditos());
    }
}
