package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmPaymentAndCreditTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ExternalPaymentService externalPaymentService;

    @InjectMocks
    private ConfirmPaymentAndCredit confirmPaymentAndCredit;

    @Test
    void execute_AcreditaSaldo_CuandoPagoEsAprobado() {
        String paymentId = "12345";
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.00");
        PaymentDetails details = new PaymentDetails(paymentId, studentId, amount, PaymentStatus.APPROVED);
        
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(BigDecimal.ZERO);

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        confirmPaymentAndCredit.execute(paymentId);

        assertEquals(amount, wallet.getSaldoCreditos());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_NoHaceNada_CuandoPagoNoEstaAprobado() {
        String paymentId = "12345";
        PaymentDetails details = new PaymentDetails(paymentId, UUID.randomUUID(), 
                new BigDecimal("1500.00"), PaymentStatus.PENDING);

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);

        confirmPaymentAndCredit.execute(paymentId);

        verify(walletRepository, never()).save(any());
    }
}
