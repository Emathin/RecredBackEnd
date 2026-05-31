package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Wallet;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelPresentialPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private CancelPresentialPurchase useCase;

    private UUID purchaseId;
    private Purchase purchase;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Student student = new Student();
        student.setId(studentId);

        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setTotalAmount(new BigDecimal("50.00"));
        purchase.setStatus(PurchaseState.ENTREGADO);

        wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setCreditBalance(new BigDecimal("100.00"));
    }

    @Test
    void execute_WhenPurchaseExists_ShouldCancelAndRefund() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId())).thenReturn(Optional.of(wallet));

        useCase.execute(purchaseId);

        assertEquals(PurchaseState.CANCELADO, purchase.getStatus());
        assertEquals(new BigDecimal("150.00"), wallet.getCreditBalance());
        verify(purchaseRepository).save(purchase);
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_WhenPurchaseNotFound_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(purchaseId));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenWalletNotFound_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> useCase.execute(purchaseId));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }
}
