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
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RevertPurchaseCancellationTest {

    private static final BigDecimal PURCHASE_AMOUNT = new BigDecimal("100.00");

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final MovimientoBilleteraRepository movimientoBilleteraRepository =
            mock(MovimientoBilleteraRepository.class);
    private final RevertPurchaseCancellation useCase = new RevertPurchaseCancellation(
            purchaseRepository,
            walletRepository,
            movimientoBilleteraRepository
    );

    private RevertPurchaseCancellationRequest request;
    private RevertPurchaseCancellationResponse response;
    private Throwable exception;
    private UUID purchaseId;
    private Alumno student;
    private Purchase purchase;
    private Billetera wallet;

    @Test
    void execute_WhenPurchaseIsCancelledAndWalletHasFunds_RevertsCancellation() {
        givenCancelledPurchase();
        givenWalletWithBalance("150.00");

        whenRevertingPurchaseCancellation();

        thenPurchaseIsPending();
        thenWalletWasDebited("50.00");
        thenWalletMovementWasRegistered();
        thenPurchaseWasSaved();
    }

    @Test
    void execute_WhenPurchaseIsNotCancelled_ThrowsException() {
        givenPendingPurchase();

        whenRevertingPurchaseCancellationFails();

        thenExceptionWasThrown();
        thenWalletWasNotRead();
        thenPurchaseWasNotSaved();
    }

    @Test
    void execute_WhenWalletHasInsufficientFunds_ThrowsException() {
        givenCancelledPurchase();
        givenWalletWithBalance("50.00");

        whenRevertingPurchaseCancellationFails();

        thenExceptionWasThrown();
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void execute_WhenPurchaseIdIsNull_ThrowsExceptionBeforeReadingRepositories() {
        givenRequestWithoutPurchaseId();

        whenRevertingPurchaseCancellationFails(IllegalArgumentException.class);

        thenExceptionMessageIs("purchaseId cannot be null");
        thenRepositoriesWereNotRead();
    }

    private void givenCancelledPurchase() {
        givenRequest();
        student = student();
        purchase = purchase(EstadoCompra.CANCELADO);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPendingPurchase() {
        givenRequest();
        student = student();
        purchase = purchase(EstadoCompra.PENDIENTE);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenWalletWithBalance(String balance) {
        wallet = wallet(balance);
        when(walletRepository.findByStudentId(student.getId())).thenReturn(Optional.of(wallet));
    }

    private void givenRequest() {
        purchaseId = randomUUID();
        request = new RevertPurchaseCancellationRequest(purchaseId);
    }

    private void givenRequestWithoutPurchaseId() {
        request = new RevertPurchaseCancellationRequest(null);
    }

    private void whenRevertingPurchaseCancellation() {
        when(purchaseRepository.save(purchase)).thenReturn(purchase);
        response = useCase.execute(request);
    }

    private void whenRevertingPurchaseCancellationFails() {
        whenRevertingPurchaseCancellationFails(IllegalStateException.class);
    }

    private <T extends Throwable> void whenRevertingPurchaseCancellationFails(
            Class<T> expectedException
    ) {
        Executable execution = () -> useCase.execute(request);
        exception = assertThrows(expectedException, execution);
    }

    private void thenPurchaseIsPending() {
        assertEquals(EstadoCompra.PENDIENTE, purchase.getStatus());
        assertEquals(EstadoCompra.PENDIENTE, response.status());
    }

    private void thenWalletWasDebited(String expectedBalance) {
        assertEquals(new BigDecimal(expectedBalance), wallet.getSaldoCreditos());
        verify(walletRepository).save(wallet);
    }

    private void thenWalletMovementWasRegistered() {
        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                PURCHASE_AMOUNT.negate(),
                "Débito por reversión de cancelación de compra"
        );
    }

    private void thenPurchaseWasSaved() {
        verify(purchaseRepository).save(purchase);
    }

    private void thenExceptionWasThrown() {
        assertEquals(IllegalStateException.class, exception.getClass());
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenRepositoriesWereNotRead() {
        verifyNoInteractions(purchaseRepository, walletRepository, movimientoBilleteraRepository);
    }

    private void thenWalletWasNotRead() {
        verify(walletRepository, never()).findByStudentId(student.getId());
    }

    private void thenWalletWasNotSaved() {
        verify(walletRepository, never()).save(wallet);
    }

    private void thenPurchaseWasNotSaved() {
        verify(purchaseRepository, never()).save(purchase);
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(randomUUID());
        return student;
    }

    private Purchase purchase(EstadoCompra status) {
        Purchase purchase = new Purchase(
                student,
                Collections.emptyMap(),
                MedioPago.CREDITOS,
                randomUUID(),
                BuyerType.STUDENT
        );
        purchase.setId(purchaseId);
        purchase.setTotalAmount(PURCHASE_AMOUNT);
        purchase.setStatus(status);
        return purchase;
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }
}
