package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ProcessPresentialPurchaseTest {

    @Mock
    private AlumnoRepository studentRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private ValidadorHorarioService timeValidator;
    @Mock
    private ValidacionRestriccionesService healthValidator;
    @Mock
    private ValidadorPresupuestoService budgetValidator;

    @InjectMocks
    private ProcessPresentialPurchase useCase;

    private UUID studentId;
    private Alumno student;
    private Billetera wallet;
    private Map<Producto, Integer> cart;
    private Producto product;
    private CategoriaProducto category;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);

        wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));

        category = new CategoriaProducto();
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

        useCase.execute(studentId, cart);

        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));
        verify(healthValidator).validarCompra(student, cart.keySet());

        Map<UUID, BigDecimal> expectedCategoryTotals = new HashMap<>();
        expectedCategoryTotals.put(category.getId(), new BigDecimal("20.00"));

        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(new BigDecimal("20.00")),
                eq(expectedCategoryTotals),
                any(LocalDateTime.class)
        );

        verify(walletRepository).save(any(Billetera.class));
        verify(purchaseRepository).save(any());
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(AlumnoNoEncontradoException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verifyNoInteractions(timeValidator, healthValidator, budgetValidator, walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenInsufficientBalance_ShouldThrowException() {
        wallet.setSaldoCreditos(new BigDecimal("10.00"));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));
        verify(healthValidator).validarCompra(student, cart.keySet());
        verify(budgetValidator).validarPresupuesto(any(), any(), any(), any());
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenHealthRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new RestriccionSaludException("")).when(healthValidator).validarCompra(student, cart.keySet());

        assertThrows(RestriccionSaludException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));
        verifyNoInteractions(budgetValidator, walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenTimeRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new RestriccionHorariaException("")).when(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));

        assertThrows(RestriccionHorariaException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verifyNoInteractions(healthValidator, budgetValidator, walletRepository, purchaseRepository);
    }
}