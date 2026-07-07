package com.recred.backend.unit.application;

import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.ProcessPresentialPurchaseRequest;
import com.recred.backend.application.ProcessPresentialPurchaseResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPresentialPurchaseTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    
    private static final String PRODUCT_NAME = "Test Product";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("10.00");
    private static final BigDecimal SUFFICIENT_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal INSUFFICIENT_BALANCE = new BigDecimal("10.00");
    private static final BigDecimal PURCHASE_TOTAL = new BigDecimal("-20.00");
    private static final String MOVEMENT_DESC = "Compra presencial en buffet";

    @Mock
    private AlumnoRepository studentRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private PurchaseValidator purchaseValidator;
    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProcessPresentialPurchase useCase;

    @Test
    void execute_ShouldProcessPurchaseAndCallValidators() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenWallet(SUFFICIENT_BALANCE);
        givenPurchaseSaved(EstadoCompra.ENTREGADO);

        ProcessPresentialPurchaseResponse response = whenExecute(request);

        thenPurchaseIsValidatedAndSaved(response, EstadoCompra.ENTREGADO);
        thenInventoryIsConsumed();
        thenWalletIsSaved();
    }

    @Test
    void execute_WhenProductRequiresPreparation_ShouldSavePendingPurchase() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(true);
        givenWallet(SUFFICIENT_BALANCE);
        givenPurchaseSaved(EstadoCompra.PENDIENTE);

        ProcessPresentialPurchaseResponse response = whenExecute(request);

        thenPurchaseIsSavedWithStatus(response, EstadoCompra.PENDIENTE);
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudentNotFound();

        Executable execution = whenExecuteExecutable(request);

        thenStudentNotFoundExceptionIsThrown(execution);
    }

    @Test
    void execute_WhenInsufficientBalance_ShouldThrowException() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenWallet(INSUFFICIENT_BALANCE);

        Executable execution = whenExecuteExecutable(request);

        thenInsufficientBalanceExceptionIsThrown(execution);
    }

    @Test
    void execute_WhenWalletNotFound_ShouldThrowException() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenWalletNotFound();

        Executable execution = whenExecuteExecutable(request);

        thenIllegalStateExceptionIsThrown(execution);
    }

    @Test
    void execute_WhenHealthRestrictionFails_ShouldThrowException() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenPurchaseValidatorFailsWithHealthRestriction();

        Executable execution = whenExecuteExecutable(request);

        thenHealthRestrictionExceptionIsThrown(execution);
    }

    @Test
    void execute_WhenTimeRestrictionFails_ShouldThrowException() {
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenPurchaseValidatorFailsWithTimeRestriction();

        Executable execution = whenExecuteExecutable(request);

        thenTimeRestrictionExceptionIsThrown(execution);
    }

    @Test
    void execute_WithMovementRepositoryAndStudentWithoutUser_ShouldRegisterSystemMovement() {
        MovimientoBilleteraRepository movementRepository = mock(MovimientoBilleteraRepository.class);
        ProcessPresentialPurchase localUseCase = buildUseCaseWithMovementRepo(movementRepository);
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenWallet(SUFFICIENT_BALANCE);
        givenPurchaseSaved(EstadoCompra.ENTREGADO);

        localUseCase.execute(request);

        thenSystemMovementIsRegistered(movementRepository);
    }

    @Test
    void execute_WithMovementRepositoryAndStudentUser_ShouldRegisterUserMovement() {
        MovimientoBilleteraRepository movementRepository = mock(MovimientoBilleteraRepository.class);
        ProcessPresentialPurchase localUseCase = buildUseCaseWithMovementRepo(movementRepository);
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(true);
        givenProduct(false);
        givenWallet(SUFFICIENT_BALANCE);
        givenPurchaseSaved(EstadoCompra.ENTREGADO);

        localUseCase.execute(request);

        thenUserMovementIsRegistered(movementRepository);
    }

    @Test
    void execute_WithoutMovementRepository_KeepsMovementRegistrationDisabled() {
        ProcessPresentialPurchase localUseCase = buildUseCaseWithMovementRepo(null);
        ProcessPresentialPurchaseRequest request = givenRequest();
        givenStudent(false);
        givenProduct(false);
        givenWallet(SUFFICIENT_BALANCE);
        givenPurchaseSaved(EstadoCompra.ENTREGADO);
        
        localUseCase.execute(request);

        thenPurchaseIsSaved();
    }

    private ProcessPresentialPurchaseRequest givenRequest() {
        return new ProcessPresentialPurchaseRequest(
                STUDENT_ID,
                List.of(new ProcessPresentialPurchaseRequest.ItemRequest(PRODUCT_ID, 2)),
                BUFFET_ID
        );
    }

    private void givenStudent(boolean withUser) {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        if (withUser) {
            student.setUsuarioId(USER_ID);
        }
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private void givenStudentNotFound() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void givenProduct(boolean requiresPreparation) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(CATEGORY_ID);
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setNombre(PRODUCT_NAME);
        product.setPrecio(PRODUCT_PRICE);
        product.setCategoria(category);
        product.setRequierePreparacion(requiresPreparation);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    private void givenWallet(BigDecimal balance) {
        Billetera wallet = new Billetera();
        wallet.setId(WALLET_ID);
        wallet.setSaldoCreditos(balance);
        when(walletRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(wallet));
    }

    private void givenWalletNotFound() {
        when(walletRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void givenPurchaseSaved(EstadoCompra status) {
        Purchase saved = new Purchase();
        saved.setId(PURCHASE_ID);
        saved.setStatus(status);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);
    }

    private void givenPurchaseValidatorFailsWithHealthRestriction() {
        doThrow(new RestriccionSaludException(""))
                .when(purchaseValidator).validate(any(Purchase.class));
    }

    private void givenPurchaseValidatorFailsWithTimeRestriction() {
        doThrow(new RestriccionHorariaException(""))
                .when(purchaseValidator).validate(any(Purchase.class));
    }

    private ProcessPresentialPurchase buildUseCaseWithMovementRepo(
            MovimientoBilleteraRepository movementRepository) {
        return new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator,
                inventoryAvailabilityService,
                movementRepository,
                productRepository
        );
    }

    // --- Helpers: When ---

    private ProcessPresentialPurchaseResponse whenExecute(
            ProcessPresentialPurchaseRequest request) {
        return useCase.execute(request);
    }

    private Executable whenExecuteExecutable(
            ProcessPresentialPurchaseRequest request) {
        return () -> useCase.execute(request);
    }

    private void thenPurchaseIsValidatedAndSaved(
            ProcessPresentialPurchaseResponse response, EstadoCompra expectedStatus) {
        verify(purchaseValidator).validate(any(Purchase.class));
        thenPurchaseIsSavedWithStatus(response, expectedStatus);
    }

    private void thenPurchaseIsSavedWithStatus(
            ProcessPresentialPurchaseResponse response, EstadoCompra expectedStatus) {
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals(expectedStatus, purchaseCaptor.getValue().getStatus());
        assertEquals(expectedStatus.name(), response.status());
        if (expectedStatus == EstadoCompra.ENTREGADO) {
            assertEquals(PURCHASE_ID, response.purchaseId());
        }
    }

    private void thenPurchaseIsSaved() {
        verify(purchaseRepository).save(any(Purchase.class));
    }

    private void thenInventoryIsConsumed() {
        ArgumentCaptor<Map<Producto, Integer>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(inventoryAvailabilityService).consumeDirectly(eq(BUFFET_ID), mapCaptor.capture());
        Map<Producto, Integer> capturedCart = mapCaptor.getValue();
        assertEquals(1, capturedCart.size());
        assertEquals(2, capturedCart.values().iterator().next());
    }

    private void thenWalletIsSaved() {
        verify(walletRepository).save(any(Billetera.class));
    }

    private void thenStudentNotFoundExceptionIsThrown(Executable execution) {
        assertThrows(AlumnoNoEncontradoException.class, execution);
        verifyNoInteractions(
                purchaseValidator, walletRepository, purchaseRepository, 
                inventoryAvailabilityService);
    }

    private void thenInsufficientBalanceExceptionIsThrown(Executable execution) {
        assertThrows(InsufficientBalanceException.class, execution);
        verify(purchaseValidator).validate(any(Purchase.class));
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verifyNoInteractions(inventoryAvailabilityService);
    }

    private void thenIllegalStateExceptionIsThrown(Executable execution) {
        assertThrows(IllegalStateException.class, execution);
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verifyNoInteractions(inventoryAvailabilityService);
    }

    private void thenHealthRestrictionExceptionIsThrown(Executable execution) {
        assertThrows(RestriccionSaludException.class, execution);
        verifyNoInteractions(walletRepository, purchaseRepository, inventoryAvailabilityService);
    }

    private void thenTimeRestrictionExceptionIsThrown(Executable execution) {
        assertThrows(RestriccionHorariaException.class, execution);
        verifyNoInteractions(walletRepository, purchaseRepository, inventoryAvailabilityService);
    }

    private void thenSystemMovementIsRegistered(MovimientoBilleteraRepository movementRepository) {
        verify(movementRepository).registrarMovimientoSistema(
                WALLET_ID,
                TipoMovimientoBilletera.COMPRA,
                PURCHASE_TOTAL,
                MOVEMENT_DESC
        );
    }

    private void thenUserMovementIsRegistered(MovimientoBilleteraRepository movementRepository) {
        verify(movementRepository).registrarMovimiento(
                WALLET_ID,
                TipoMovimientoBilletera.COMPRA,
                PURCHASE_TOTAL,
                MOVEMENT_DESC,
                USER_ID
        );
    }
}
