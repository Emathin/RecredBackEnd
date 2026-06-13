package com.recred.backend.domain.services;

import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.BuffetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PurchaseValidatorImplTest {

    @Mock
    private ValidadorHorarioService validadorHorarioService;
    @Mock
    private ValidacionRestriccionesService validacionRestriccionesService;
    @Mock
    private ValidadorPresupuestoService validadorPresupuestoService;
    @Mock
    private ValidadorCalendarioService validadorCalendarioService;
    @Mock
    private BuffetRepository buffetRepository;

    @InjectMocks
    private PurchaseValidatorImpl purchaseValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void validate_shouldCallAllValidators_whenPurchaseIsAdvance() {
        // Arrange
        UUID buffetId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.now().plusDays(1);

        Alumno student = new Alumno();
        student.setId(studentId);

        Categoria categoria = new Categoria();
        categoria.setId(UUID.randomUUID());

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setPrecio(BigDecimal.valueOf(100));
        producto.setCategoria(categoria);

        Map<Producto, Integer> items = new HashMap<>();
        items.put(producto, 2);

        Purchase purchase = new Purchase();
        purchase.setStudent(student);
        purchase.setItems(items);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setBuffetId(buffetId);
        purchase.setPickupDate(pickupDate);

        Buffet buffet = new Buffet();
        buffet.setId(buffetId);

        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));

        // Act
        purchaseValidator.validate(purchase);

        // Assert
        verify(validadorCalendarioService, times(1)).validarVenta(buffet, pickupDate);
        verify(validadorHorarioService, times(1)).validarCompra(eq(student), eq(items.keySet()), any());
        verify(validacionRestriccionesService, times(1)).validarCompra(student, items.keySet());
        verify(validadorPresupuestoService, times(1)).validarPresupuesto(eq(studentId), any(), any(), any());
    }

    @Test
    void validate_shouldNotCallCalendarValidator_whenPurchaseIsSpontaneous() {
        // Arrange
        UUID buffetId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Alumno student = new Alumno();
        student.setId(studentId);

        Categoria categoria = new Categoria();
        categoria.setId(UUID.randomUUID());

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setPrecio(BigDecimal.valueOf(50));
        producto.setCategoria(categoria);

        Map<Producto, Integer> items = new HashMap<>();
        items.put(producto, 1);

        Purchase purchase = new Purchase();
        purchase.setStudent(student);
        purchase.setItems(items);
        purchase.setTipo(TipoCompra.ESPONTANEA);
        purchase.setBuffetId(buffetId);

        // Act
        purchaseValidator.validate(purchase);

        // Assert
        verify(validadorCalendarioService, never()).validarVenta(any(), any());
        verify(validadorHorarioService, times(1)).validarCompra(eq(student), eq(items.keySet()), any());
        verify(validacionRestriccionesService, times(1)).validarCompra(student, items.keySet());
        verify(validadorPresupuestoService, times(1)).validarPresupuesto(eq(studentId), any(), any(), any());
    }
}
