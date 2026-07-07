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
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private final UUID STUDENT_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID PROMOTION_ID = UUID.randomUUID();

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
        CheckBudgetDatesRequest request = givenRequestWithEmptyCart();
        givenBudgetValidatorAllows();

        CheckBudgetDatesResponse response = whenCheckBudgetDates(request);

        thenDatesAreValid(response, 2);
    }

    @Test
    void execute_WhenBudgetExceeded_ShouldReturnBlockedWithReason() {
        String expectedReason = "Supera el límite diario de Gaseosas: disponible $900";
        
        givenProductExistsWithPrice(500);
        CheckBudgetDatesRequest request = givenRequestWithProduct(2);
        givenBudgetValidatorBlocksWithReason(new BigDecimal("1000"), expectedReason);

        CheckBudgetDatesResponse response = whenCheckBudgetDates(request);

        thenDateIsBlockedWithReason(response, expectedReason);
    }

    @Test
    void execute_WhenProductDoesNotExist_ShouldThrowException() {
        CheckBudgetDatesRequest request = givenRequestWithProduct();
        givenProductAndPromotionDoNotExist();

        Executable execution = () -> whenCheckBudgetDates(request);

        thenExceptionIsThrown(execution, "Product or Promotion not found: " + PRODUCT_ID);
    }

    @Test
    void execute_WhenProductPriceIsNull_ShouldUseZeroAsItemTotal() {
        givenProductExistsWithPrice(null);
        CheckBudgetDatesRequest request = givenRequestWithProduct(3);
        givenBudgetValidatorAllows();

        CheckBudgetDatesResponse response = whenCheckBudgetDates(request);

        thenDatesAreValid(response, 1);
    }

    @Test
    void execute_WhenCartContainsPromotion_ShouldCalculateDiscountedPricesAndCheckBudget() {
        givenPromotionExistsWithDiscountAndProductPrice(20, 1000);
        CheckBudgetDatesRequest request = givenRequestWithPromotion(2);
        givenBudgetValidatorAllowsWithTotal(new BigDecimal("1600.00")); 

        CheckBudgetDatesResponse response = whenCheckBudgetDates(request);

        thenDatesAreValid(response, 1);
    }

    private CheckBudgetDatesResponse whenCheckBudgetDates(CheckBudgetDatesRequest request) {
        return checkBudgetDates.execute(request);
    }

    private CheckBudgetDatesRequest givenRequestWithEmptyCart() {
        LocalDate date1 = LocalDate.of(2026, 6, 15);
        LocalDate date2 = LocalDate.of(2026, 6, 16);
        return new CheckBudgetDatesRequest(List.of(date1, date2), List.of())
                .withStudentId(STUDENT_ID);
    }

    private CheckBudgetDatesRequest givenRequestWithProduct(int quantity) {
        LocalDate defaultDate = LocalDate.of(2026, 6, 15);
        return new CheckBudgetDatesRequest(
                List.of(defaultDate),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(PRODUCT_ID, quantity))
        ).withStudentId(STUDENT_ID);
    }

    private CheckBudgetDatesRequest givenRequestWithProduct() {
        return givenRequestWithProduct(1);
    }

    private CheckBudgetDatesRequest givenRequestWithPromotion(int quantity) {
        LocalDate defaultDate = LocalDate.of(2026, 6, 15);
        return new CheckBudgetDatesRequest(
                List.of(defaultDate),
                List.of(new CheckBudgetDatesRequest.CartItemRequest(PROMOTION_ID, quantity))
        ).withStudentId(STUDENT_ID);
    }

    private void givenProductExistsWithPrice(Integer price) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(UUID.randomUUID());
        categoria.setDescripcion("Gaseosas");

        Producto producto = new Producto();
        producto.setId(PRODUCT_ID);
        producto.setPrecio(price != null ? BigDecimal.valueOf(price) : null);
        producto.setCategoria(categoria);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(producto));
    }

    private void givenProductAndPromotionDoNotExist() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(promotionRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
    }

    private void givenPromotionExistsWithDiscountAndProductPrice(int discount, int productPrice) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(UUID.randomUUID());

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setPrecio(BigDecimal.valueOf(productPrice));
        producto.setCategoria(categoria);

        Promotion promotion = new Promotion();
        promotion.setId(PROMOTION_ID);
        promotion.setDiscountPercentage(BigDecimal.valueOf(discount));
        promotion.setProducts(List.of(producto));

        when(productRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());
        when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion));
    }

    private void givenBudgetValidatorAllows() {
        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(STUDENT_ID), any(), any(), any(LocalDateTime.class)
        )).thenReturn(Optional.empty());
    }

    private void givenBudgetValidatorAllowsWithTotal(BigDecimal total) {
        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(STUDENT_ID), eq(total), any(), any(LocalDateTime.class)
        )).thenReturn(Optional.empty());
    }

    private void givenBudgetValidatorBlocksWithReason(BigDecimal total, String reason) {
        when(budgetValidator.obtenerMotivoBloqueoPresupuesto(
                eq(STUDENT_ID), eq(total), any(), any(LocalDateTime.class)
        )).thenReturn(Optional.of(reason));
    }

    private void thenDatesAreValid(CheckBudgetDatesResponse response, int expectedCount) {
        assertEquals(expectedCount, response.validationResults().size());
        for (CheckBudgetDatesResponse.DateBudgetStatus result : response.validationResults()) {
            assertFalse(result.blocked());
            assertNull(result.reason());
        }
    }

    private void thenDateIsBlockedWithReason(
            CheckBudgetDatesResponse response, String expectedReason) {
        assertEquals(1, response.validationResults().size());
        assertTrue(response.validationResults().get(0).blocked());
        assertEquals(expectedReason, response.validationResults().get(0).reason());
    }

    private void thenExceptionIsThrown(Executable execution, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals(expectedMessage, exception.getMessage());
    }
}
