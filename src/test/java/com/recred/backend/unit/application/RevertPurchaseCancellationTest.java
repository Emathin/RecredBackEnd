package com.recred.backend.unit.application;

import com.recred.backend.application.RevertPurchaseCancellation;
import com.recred.backend.application.RevertPurchaseCancellationRequest;
import com.recred.backend.application.RevertPurchaseCancellationResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevertPurchaseCancellationTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovimientoBilleteraRepository movimientoBilleteraRepository;

    private RevertPurchaseCancellation revertPurchaseCancellation;

    @BeforeEach
    void setUp() {
        revertPurchaseCancellation = new RevertPurchaseCancellation(
                purchaseRepository,
                walletRepository,
                movimientoBilleteraRepository
        );
    }

    @Test
    void shouldRevertCancellationSuccessfully() {
        UUID purchaseId = UUID.randomUUID();
        RevertPurchaseCancellationRequest request = new RevertPurchaseCancellationRequest(purchaseId);
        
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        
        Purchase purchase = new Purchase(student, Collections.emptyMap(), MedioPago.CREDITOS, UUID.randomUUID(), BuyerType.STUDENT);
        purchase.setId(purchaseId);
        purchase.setTotalAmount(new BigDecimal("100.00"));
        purchase.setStatus(EstadoCompra.CANCELADO);
        
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("150.00"));
        
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(student.getId())).thenReturn(Optional.of(wallet));
        when(purchaseRepository.save(purchase)).thenReturn(purchase);
        
        RevertPurchaseCancellationResponse response = revertPurchaseCancellation.execute(request);
        
        assertEquals(EstadoCompra.PENDIENTE, purchase.getStatus());
        assertEquals(new BigDecimal("50.00"), wallet.getSaldoCreditos());
        assertEquals(EstadoCompra.PENDIENTE, response.status());
        verify(walletRepository).save(wallet);
        verify(purchaseRepository).save(purchase);
    }
    
    @Test
    void shouldThrowExceptionIfPurchaseNotCancelled() {
        UUID purchaseId = UUID.randomUUID();
        RevertPurchaseCancellationRequest request = new RevertPurchaseCancellationRequest(purchaseId);
        
        Purchase purchase = new Purchase();
        purchase.setStatus(EstadoCompra.PENDIENTE);
        
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        
        Executable execution = () -> revertPurchaseCancellation.execute(request);

        assertThrows(IllegalStateException.class, execution);
    }
    
    @Test
    void shouldThrowExceptionIfInsufficientFunds() {
        UUID purchaseId = UUID.randomUUID();
        RevertPurchaseCancellationRequest request = new RevertPurchaseCancellationRequest(purchaseId);
        
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        
        Purchase purchase = new Purchase(student, Collections.emptyMap(), MedioPago.CREDITOS, UUID.randomUUID(), BuyerType.STUDENT);
        purchase.setId(purchaseId);
        purchase.setTotalAmount(new BigDecimal("100.00"));
        purchase.setStatus(EstadoCompra.CANCELADO);
        
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("50.00"));
        
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(student.getId())).thenReturn(Optional.of(wallet));
        
        Executable execution = () -> revertPurchaseCancellation.execute(request);

        assertThrows(IllegalStateException.class, execution);
    }
}
