package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setCreditBalance(new BigDecimal("100.00"));
    }

    @Test
    void hasSufficientBalance_WhenBalanceIsGreater_ShouldReturnTrue() {
        assertTrue(wallet.hasSufficientBalance(new BigDecimal("50.00")));
    }

    @Test
    void hasSufficientBalance_WhenBalanceIsEqual_ShouldReturnTrue() {
        assertTrue(wallet.hasSufficientBalance(new BigDecimal("100.00")));
    }

    @Test
    void hasSufficientBalance_WhenBalanceIsLess_ShouldReturnFalse() {
        assertFalse(wallet.hasSufficientBalance(new BigDecimal("150.00")));
    }

    @Test
    void debit_WithSufficientBalance_ShouldSubtractAmount() {
        wallet.debit(new BigDecimal("40.00"));
        assertEquals(new BigDecimal("60.00"), wallet.getCreditBalance());
    }

    @Test
    void debit_WithInsufficientBalance_ShouldThrowException() {
        BigDecimal amountToDebit = new BigDecimal("150.00");
        
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            wallet.debit(amountToDebit);
        });
        
        assertEquals("Not enough balance to perform this debit operation.", exception.getMessage());
        assertEquals(new BigDecimal("100.00"), wallet.getCreditBalance());
    }

    @Test
    void credit_ShouldAddAmount() {
        wallet.credit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), wallet.getCreditBalance());
    }
}
