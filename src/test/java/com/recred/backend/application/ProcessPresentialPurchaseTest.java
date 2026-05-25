package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
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

    @InjectMocks
    private ProcessPresentialPurchase useCase;

    private UUID studentId;
    private Alumno student;
    private Billetera wallet;
    private Map<Producto, Integer> cart;
    private Producto product;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);

        wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));

        product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Test Product");
        product.setPrecio(new BigDecimal("10.00"));

        cart = new HashMap<>();
        cart.put(product, 2); // Total: 20.00
    }

    @Test
    void execute_WithValidData_ShouldProcessPurchase() {
        // Given
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        // When
        useCase.execute(studentId, cart);

        // Then
        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));
        verify(healthValidator).validarCompra(student, cart.keySet());
        verify(walletRepository).save(any(Billetera.class));
        verify(purchaseRepository).save(any());
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        // Given
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AlumnoNoEncontradoException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verifyNoInteractions(timeValidator, healthValidator, walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenInsufficientBalance_ShouldThrowException() {
        // Given
        wallet.setSaldoCreditos(new BigDecimal("10.00")); // Not enough for a 20.00 purchase
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        // When & Then
        assertThrows(InsufficientBalanceException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));
        verify(healthValidator).validarCompra(student, cart.keySet());
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenHealthRestrictionFails_ShouldThrowException() {
        // Given
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new RestriccionSaludException("")).when(healthValidator).validarCompra(student, cart.keySet());

        // When & Then
        assertThrows(RestriccionSaludException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));
        verifyNoInteractions(walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenTimeRestrictionFails_ShouldThrowException() {
        // Given
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new RestriccionHorariaException("")).when(timeValidator).validarCompra(eq(student), eq(cart.keySet()), any(LocalDateTime.class));

        // When & Then
        assertThrows(RestriccionHorariaException.class, () -> {
            useCase.execute(studentId, cart);
        });

        verifyNoInteractions(healthValidator, walletRepository, purchaseRepository);
    }
}
