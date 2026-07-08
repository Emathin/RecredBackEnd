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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmPaymentAndCreditTest {

    private static final String PAYMENT_ID = "12345";
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("1500.00");
    private static final String PAYMENT_CREDIT_DESCRIPTION =
            "Acreditacion de pago Mercado Pago: " + PAYMENT_ID;

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private ExternalPaymentService externalPaymentService;
    @Mock
    private MovimientoBilleteraRepository movimientoBilleteraRepository;

    private ConfirmPaymentAndCredit useCase;
    private UUID studentId;
    private Billetera wallet;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        wallet = wallet();
        useCase = new ConfirmPaymentAndCredit(
                walletRepository,
                externalPaymentService,
                movimientoBilleteraRepository
        );
    }

    @Test
    void approvedPaymentCreditsStudentWallet() {
        givenPaymentHasStatus(PaymentStatus.APPROVED);
        givenStudentWalletExists();

        whenConfirmingPayment();

        thenWalletWasCredited();
        thenWalletWasSaved();
    }

    @Test
    void approvedPaymentCanBeCreditedWithoutMovementRepository() {
        useCase = new ConfirmPaymentAndCredit(walletRepository, externalPaymentService);
        givenPaymentHasStatus(PaymentStatus.APPROVED);
        givenStudentWalletExists();

        whenConfirmingPayment();

        thenWalletWasCredited();
        thenWalletWasSaved();
        thenMovementWasNotRegistered();
    }

    @Test
    void approvedPaymentRegistersSystemMovement() {
        givenPaymentHasStatus(PaymentStatus.APPROVED);
        givenStudentWalletExists();

        whenConfirmingPayment();

        thenPaymentCreditMovementWasRegistered();
    }

    @Test
    void approvedPaymentWithoutWalletFailsBeforeSaving() {
        givenPaymentHasStatus(PaymentStatus.APPROVED);
        givenStudentWalletDoesNotExist();

        whenConfirmingPaymentFails(RuntimeException.class);

        thenErrorMessageIs("Billetera no encontrada para alumno: " + studentId);
        thenWalletWasNotSaved();
        thenMovementWasNotRegistered();
    }

    @Test
    void pendingPaymentDoesNotCreditWallet() {
        givenPaymentHasStatus(PaymentStatus.PENDING);

        whenConfirmingPayment();

        thenWalletWasNotSaved();
        thenMovementWasNotRegistered();
    }

    private void givenPaymentHasStatus(PaymentStatus status) {
        when(externalPaymentService.getPaymentDetails(PAYMENT_ID))
                .thenReturn(new PaymentDetails(
                        PAYMENT_ID,
                        studentId,
                        PAYMENT_AMOUNT,
                        status));
    }

    private void givenStudentWalletExists() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
    }

    private void givenStudentWalletDoesNotExist() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
    }

    private void whenConfirmingPayment() {
        useCase.execute(defaultRequest());
    }

    private <T extends Throwable> void whenConfirmingPaymentFails(Class<T> exceptionType) {
        Executable execution = () -> useCase.execute(defaultRequest());
        thrownException = assertThrows(exceptionType, execution);
    }

    private void thenWalletWasCredited() {
        assertEquals(PAYMENT_AMOUNT, wallet.getSaldoCreditos());
    }

    private void thenWalletWasSaved() {
        verify(walletRepository).save(wallet);
    }

    private void thenWalletWasNotSaved() {
        verify(walletRepository, never()).save(any());
    }

    private void thenPaymentCreditMovementWasRegistered() {
        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.CARGA,
                PAYMENT_AMOUNT,
                PAYMENT_CREDIT_DESCRIPTION
        );
    }

    private void thenMovementWasNotRegistered() {
        verifyNoInteractions(movimientoBilleteraRepository);
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private ConfirmPaymentAndCreditRequest defaultRequest() {
        return new ConfirmPaymentAndCreditRequest(PAYMENT_ID);
    }

    private Billetera wallet() {
        Billetera studentWallet = new Billetera();
        studentWallet.setId(UUID.randomUUID());
        studentWallet.setSaldoCreditos(BigDecimal.ZERO);
        return studentWallet;
    }
}
