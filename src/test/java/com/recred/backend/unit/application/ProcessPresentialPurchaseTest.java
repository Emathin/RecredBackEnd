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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProcessPresentialPurchase useCase;

    private UUID studentId;
    private Alumno student;
    private Billetera wallet;
    private ProcessPresentialPurchaseRequest request;
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

        request = new ProcessPresentialPurchaseRequest(
                studentId,
                List.of(new ProcessPresentialPurchaseRequest.ItemRequest(product.getId(), 2)),
                buffetId
        );
    }

    @Test
    void execute_WithValidData_ShouldProcessPurchaseAndCallValidators() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        
        Purchase saved = new Purchase();
        saved.setId(UUID.randomUUID());
        saved.setStatus(EstadoCompra.ENTREGADO);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);

        ProcessPresentialPurchaseResponse response = useCase.execute(request);

        verify(purchaseValidator).validate(any(Purchase.class));

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(walletRepository).save(wallet);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        
        Map<Producto, Integer> expectedCart = new HashMap<>();
        expectedCart.put(product, 2);
        verify(inventoryAvailabilityService).consumeDirectly(buffetId, expectedCart);
        
        assertEquals(saved.getId(), response.purchaseId());
        assertEquals(saved.getStatus().name(), response.status());
    }

    @Test
    void execute_WhenProductRequiresPreparation_ShouldSavePendingPurchase() {
        product.setRequierePreparacion(true);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        
        Purchase saved = new Purchase();
        saved.setId(UUID.randomUUID());
        saved.setStatus(EstadoCompra.PENDIENTE);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);

        ProcessPresentialPurchaseResponse response = useCase.execute(request);

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals(EstadoCompra.PENDIENTE, purchaseCaptor.getValue().getStatus());
        assertEquals("PENDIENTE", response.status());
    }

    @Test
    void execute_WithBuffetId_ShouldConsumeInventoryDirectly() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        
        Purchase saved = new Purchase();
        saved.setId(UUID.randomUUID());
        saved.setStatus(EstadoCompra.ENTREGADO);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);

        useCase.execute(request);

        Map<Producto, Integer> expectedCart = new HashMap<>();
        expectedCart.put(product, 2);
        verify(inventoryAvailabilityService).consumeDirectly(buffetId, expectedCart);
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        ProcessPresentialPurchaseRequest badRequest = new ProcessPresentialPurchaseRequest(
                studentId, request.cart(), null
        );
        Executable execution = () -> useCase.execute(badRequest);

        assertThrows(IllegalArgumentException.class, execution);
        verifyNoInteractions(studentRepository, walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        assertThrows(AlumnoNoEncontradoException.class, execution);
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
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        Executable execution = () -> useCase.execute(request);

        assertThrows(InsufficientBalanceException.class, execution);
        verify(purchaseValidator).validate(any(Purchase.class));
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verifyNoInteractions(inventoryAvailabilityService);
    }

    @Test
    void execute_WhenWalletNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        assertThrows(IllegalStateException.class, execution);
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verifyNoInteractions(inventoryAvailabilityService);
    }

    @Test
    void execute_WhenHealthRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        doThrow(new RestriccionSaludException(""))
                .when(purchaseValidator)
                .validate(any(Purchase.class));

        Executable execution = () -> useCase.execute(request);

        assertThrows(RestriccionSaludException.class, execution);
        verifyNoInteractions(walletRepository, purchaseRepository, inventoryAvailabilityService);
    }

    @Test
    void execute_WhenTimeRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        doThrow(new RestriccionHorariaException(""))
                .when(purchaseValidator)
                .validate(any(Purchase.class));

        Executable execution = () -> useCase.execute(request);

        assertThrows(RestriccionHorariaException.class, execution);
        verifyNoInteractions(walletRepository, purchaseRepository, inventoryAvailabilityService);
    }

    @Test
    void execute_WithMovementRepositoryAndStudentWithoutUser_ShouldRegisterSystemMovement() {
        MovimientoBilleteraRepository movementRepository =
                mock(MovimientoBilleteraRepository.class);
        ProcessPresentialPurchase localUseCase = new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator,
                inventoryAvailabilityService,
                movementRepository,
                productRepository
        );
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        Purchase saved = new Purchase();
        saved.setId(UUID.randomUUID());
        saved.setStatus(EstadoCompra.ENTREGADO);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);

        localUseCase.execute(request);

        verify(movementRepository).registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                new BigDecimal("-20.00"),
                "Compra presencial en buffet"
        );
    }

    @Test
    void execute_WithMovementRepositoryAndStudentUser_ShouldRegisterUserMovement() {
        UUID userId = UUID.randomUUID();
        student.setUsuarioId(userId);
        MovimientoBilleteraRepository movementRepository =
                mock(MovimientoBilleteraRepository.class);
        ProcessPresentialPurchase localUseCase = new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator,
                inventoryAvailabilityService,
                movementRepository,
                productRepository
        );
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        Purchase saved = new Purchase();
        saved.setId(UUID.randomUUID());
        saved.setStatus(EstadoCompra.ENTREGADO);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);

        localUseCase.execute(request);

        verify(movementRepository).registrarMovimiento(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                new BigDecimal("-20.00"),
                "Compra presencial en buffet",
                userId
        );
    }

    @Test
    void constructorWithoutMovementRepositoryKeepsMovementRegistrationDisabled() {
        ProcessPresentialPurchase localUseCase = new ProcessPresentialPurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                purchaseValidator,
                inventoryAvailabilityService,
                null,
                productRepository
        );
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        Purchase saved = new Purchase();
        saved.setId(UUID.randomUUID());
        saved.setStatus(EstadoCompra.ENTREGADO);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(saved);
        
        localUseCase.execute(request);

        verify(purchaseRepository).save(any(Purchase.class));
    }
}
