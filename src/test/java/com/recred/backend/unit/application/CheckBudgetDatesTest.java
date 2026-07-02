package com.recred.backend.unit.application;

import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.application.CheckBudgetDatesRequest;
import com.recred.backend.application.CheckBudgetDatesResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckBudgetDatesTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ValidadorPresupuestoService budgetValidator;

    @InjectMocks
    private CheckBudgetDates checkBudgetDates;

    @Test
    void execute_WhenCartIsEmpty_ShouldReturnAllDatesAsValid() {
        UUID studentId = UUID.randomUUID();
        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("2026-06-15", "2026-06-16"),
                List.of()
        );

        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(studentId),
                eq(BigDecimal.ZERO),
                any(),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        CheckBudgetDatesResponse response = checkBudgetDates.execute(studentId, request);

        assertEquals(2, response.validationResults().size());
        assertFalse(response.validationResults().get(0).blocked());
        assertNull(response.validationResults().get(0).reason());
        assertFalse(response.validationResults().get(1).blocked());
        assertNull(response.validationResults().get(1).reason());
    }

    @Test
    void execute_WhenDateIsInvalid_ShouldReturnBlockedWithReason() {
        UUID studentId = UUID.randomUUID();
        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("invalid-date"),
                List.of()
        );

        CheckBudgetDatesResponse response = checkBudgetDates.execute(studentId, request);

        assertEquals(1, response.validationResults().size());
        assertTrue(response.validationResults().get(0).blocked());
        assertEquals("Formato de fecha inválido.",
                response.validationResults().get(0).reason());
    }

    @Test
    void execute_WhenBudgetExceeded_ShouldReturnBlockedWithReason() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(categoryId);
        categoria.setDescripcion("Gaseosas");

        Producto producto = new Producto();
        producto.setId(productId);
        producto.setPrecio(BigDecimal.valueOf(500));
        producto.setCategoria(categoria);

        when(productRepository.findById(productId)).thenReturn(Optional.of(producto));

        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("2026-06-15"),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(productId, 2))
        );

        String expectedReason = "Supera el límite diario de Gaseosas: disponible $900";
        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(studentId),
                eq(BigDecimal.valueOf(1000)),
                any(),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(expectedReason));

        CheckBudgetDatesResponse response = checkBudgetDates.execute(studentId, request);

        assertEquals(1, response.validationResults().size());
        assertTrue(response.validationResults().get(0).blocked());
        assertEquals(expectedReason, response.validationResults().get(0).reason());
    }

    @Test
    void execute_WhenProductDoesNotExist_ShouldThrowException() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("2026-06-15"),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(productId, 1))
        );

        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(promotionRepository.findById(productId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> checkBudgetDates.execute(studentId, request)
        );
        assertEquals("Product or Promotion not found: " + productId, exception.getMessage());
    }


    @Test
    void execute_WhenProductPriceIsNull_ShouldUseZeroAsItemTotal() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(categoryId);
        Producto producto = new Producto();
        producto.setId(productId);
        producto.setPrecio(null);
        producto.setCategoria(categoria);
        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("2026-06-15"),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(productId, 3))
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(producto));
        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(studentId),
                eq(BigDecimal.ZERO),
                any(),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        CheckBudgetDatesResponse response = checkBudgetDates.execute(studentId, request);

        assertEquals(1, response.validationResults().size());
        assertFalse(response.validationResults().get(0).blocked());
    }

    @Test
    void execute_WhenCartContainsPromotion_ShouldCalculateDiscountedPricesAndCheckBudget() {
        UUID studentId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(categoryId);

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setPrecio(BigDecimal.valueOf(1000));
        producto.setCategoria(categoria);

        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setDiscountPercentage(BigDecimal.valueOf(20));
        promotion.setProducts(List.of(producto));

        when(productRepository.findById(promotionId)).thenReturn(Optional.empty());
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));

        CheckBudgetDatesRequest request = new CheckBudgetDatesRequest(
                List.of("2026-06-15"),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(promotionId, 2))
        );

        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(studentId),
                eq(BigDecimal.valueOf(1600).setScale(2)),
                any(),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        CheckBudgetDatesResponse response = checkBudgetDates.execute(studentId, request);

        assertEquals(1, response.validationResults().size());
        assertFalse(response.validationResults().get(0).blocked());
    }
}
