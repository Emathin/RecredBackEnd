package com.recred.backend.unit.application;

import com.recred.backend.application.ConfirmPaymentAndCredit;
import com.recred.backend.application.ConfirmPaymentAndCreditRequest;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
        ConfirmPaymentAndCreditRequest request = new ConfirmPaymentAndCreditRequest(paymentId);
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.00");
        PaymentDetails details = new PaymentDetails(paymentId, studentId, amount, PaymentStatus.APPROVED);
        
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(BigDecimal.ZERO);

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        confirmPaymentAndCredit.execute(request);

        assertEquals(amount, wallet.getSaldoCreditos());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_WithTwoArgumentConstructor_ShouldCreditApprovedPayment() {
        String paymentId = "12345";
        ConfirmPaymentAndCreditRequest request = new ConfirmPaymentAndCreditRequest(paymentId);
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.00");
        PaymentDetails details = new PaymentDetails(paymentId, studentId, amount,
                PaymentStatus.APPROVED);
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(BigDecimal.ZERO);
        ConfirmPaymentAndCredit useCase = new ConfirmPaymentAndCredit(
                walletRepository,
                externalPaymentService
        );

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(request);

        assertEquals(amount, wallet.getSaldoCreditos());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_AprobadoConMovimientoRepository_RegistraMovimientoSistema() {
        String paymentId = "12345";
        ConfirmPaymentAndCreditRequest request = new ConfirmPaymentAndCreditRequest(paymentId);
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1500.00");
        PaymentDetails details = new PaymentDetails(paymentId, studentId, amount,
                PaymentStatus.APPROVED);
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(BigDecimal.ZERO);
        MovimientoBilleteraRepository movimientoBilleteraRepository =
                mock(MovimientoBilleteraRepository.class);
        ConfirmPaymentAndCredit useCase = new ConfirmPaymentAndCredit(
                walletRepository,
                externalPaymentService,
                movimientoBilleteraRepository
        );

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(request);

        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                eq(wallet.getId()),
                eq(TipoMovimientoBilletera.CARGA),
                eq(amount),
                eq("Acreditacion de pago Mercado Pago: " + paymentId)
        );
    }

    @Test
    void execute_PagoAprobadoSinBilletera_DeberiaLanzarExcepcion() {
        String paymentId = "12345";
        ConfirmPaymentAndCreditRequest request = new ConfirmPaymentAndCreditRequest(paymentId);
        UUID studentId = UUID.randomUUID();
        PaymentDetails details = new PaymentDetails(paymentId, studentId,
                new BigDecimal("1500.00"), PaymentStatus.APPROVED);

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        Executable execution = () -> confirmPaymentAndCredit.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_NoHaceNada_CuandoPagoNoEstaAprobado() {
        String paymentId = "12345";
        ConfirmPaymentAndCreditRequest request = new ConfirmPaymentAndCreditRequest(paymentId);
        PaymentDetails details = new PaymentDetails(paymentId, UUID.randomUUID(), 
                new BigDecimal("1500.00"), PaymentStatus.PENDING);

        when(externalPaymentService.getPaymentDetails(paymentId)).thenReturn(details);

        confirmPaymentAndCredit.execute(request);

        verify(walletRepository, never()).save(any());
    }
}
