package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PurchaseValidatorImplTest {

    @Mock
    private TimeRestrictionValidatorService timeRestrictionValidatorService;

    @Mock
    private HealthRestrictionValidatorService healthRestrictionValidatorService;

    @Mock
    private BudgetValidatorService budgetValidatorService;

    @Test
    void validate_ShouldDelegateRestrictionsAndBudgetWithAmountsByCategory() {
        UUID studentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);
        Producto product = product(categoryId, new BigDecimal("300.00"));
        Purchase purchase = new Purchase(
                student,
                Map.of(product, 2),
                MedioPago.CREDITOS,
                studentId,
                null
        );
        purchase.setTotalAmount(new BigDecimal("600.00"));
        PurchaseValidatorImpl validator = new PurchaseValidatorImpl(
                timeRestrictionValidatorService,
                healthRestrictionValidatorService,
                budgetValidatorService
        );

        validator.validate(purchase);

        verify(timeRestrictionValidatorService).validatePurchase(
                eq(student),
                eq(purchase.getItems().keySet()),
                any(LocalDateTime.class)
        );
        verify(healthRestrictionValidatorService).validatePurchase(
                student,
                purchase.getItems().keySet()
        );
        ArgumentCaptor<Map<UUID, BigDecimal>> amountsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(budgetValidatorService).validateBudget(
                eq(studentId),
                eq(new BigDecimal("600.00")),
                amountsCaptor.capture(),
                any(LocalDateTime.class)
        );
        assertEquals(new BigDecimal("600.00"), amountsCaptor.getValue().get(categoryId));
    }

    private Producto product(UUID categoryId, BigDecimal price) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setCategoria(category);
        product.setNombre("Alfajor");
        product.setPrecio(price);
        product.setRequierePreparacion(false);
        return product;
    }
}
