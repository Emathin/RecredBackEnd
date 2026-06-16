package com.recred.backend.unit.application;

import com.recred.backend.application.ProcessPresentialPurchase;
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
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPresentialPurchaseTest {

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

    @InjectMocks
    private ProcessPresentialPurchase useCase;

    private UUID studentId;
    private Alumno student;
    private Billetera wallet;
    private Map<Producto, Integer> cart;
    private Producto product;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);

        wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));

        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());

        product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Test Product");
        product.setPrecio(new BigDecimal("10.00"));
        product.setCategoria(category);

        cart = new HashMap<>();
        cart.put(product, 2);
    }

    @Test
    void execute_WithValidData_ShouldProcessPurchaseAndCallValidators() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart, buffetId);

        verify(purchaseValidator).validate(any(Purchase.class));

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(walletRepository).save(wallet);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        verify(inventoryAvailabilityService).consumeDirectly(buffetId, cart);
        assertEquals(EstadoCompra.ENTREGADO, purchaseCaptor.getValue().getStatus());
    }

    @Test
    void execute_WhenProductRequiresPreparation_ShouldSavePendingPurchase() {
        product.setRequierePreparacion(true);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart, buffetId);

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals(EstadoCompra.PENDIENTE, purchaseCaptor.getValue().getStatus());
    }

    @Test
    void execute_WithBuffetId_ShouldConsumeInventoryDirectly() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart, buffetId);

        verify(inventoryAvailabilityService).consumeDirectly(buffetId, cart);
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(studentId, cart, null));

        verifyNoInteractions(studentRepository, walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(AlumnoNoEncontradoException.class,
                () -> useCase.execute(studentId, cart, buffetId));

        verifyNoInteractions(
                purchaseValidator,
                walletRepository,
                purchaseRepository,
                inventoryAvailabilityService);
    }

    @Test
    void execute_WhenInsufficientBalance_ShouldThrowException() {
        wallet.setSaldoCreditos(new BigDecimal("10.00"));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class,
                () -> useCase.execute(studentId, cart, buffetId));

        verify(purchaseValidator).validate(any(Purchase.class));
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verifyNoInteractions(inventoryAvailabilityService);
    }

    @Test
    void execute_WhenWalletNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(studentId, cart, buffetId));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verifyNoInteractions(inventoryAvailabilityService);
    }

    @Test
    void execute_WhenHealthRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new RestriccionSaludException(""))
                .when(purchaseValidator)
                .validate(any(Purchase.class));

        assertThrows(RestriccionSaludException.class,
                () -> useCase.execute(studentId, cart, buffetId));

        verifyNoInteractions(walletRepository, purchaseRepository, inventoryAvailabilityService);
    }

    @Test
    void execute_WhenTimeRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new RestriccionHorariaException(""))
                .when(purchaseValidator)
                .validate(any(Purchase.class));

        assertThrows(RestriccionHorariaException.class,
                () -> useCase.execute(studentId, cart, buffetId));

        verifyNoInteractions(walletRepository, purchaseRepository, inventoryAvailabilityService);
    }
}
