package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.PaymentMethod;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.PurchaseDetailEntity;
import com.recred.backend.infrastructure.entities.PresentialPurchaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentConsumptionJpaRepositoryTest {

    @Test
    void mapPurchaseEntitiesWithProductsSortedByProductName() {
        StudentConsumptionJpaRepository repository = mock(
                StudentConsumptionJpaRepository.class,
                CALLS_REAL_METHODS
        );
        UUID studentId = UUID.randomUUID();
        List<PurchaseState> statuses = List.of(PurchaseState.ENTREGADO);
        LocalDateTime startDateTime = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2026, 5, 31, 23, 59);
        PresentialPurchaseEntity purchase = purchase();

        when(repository.findPurchaseEntitiesByStudentIdAndDateRange(
                studentId,
                statuses,
                startDateTime,
                endDateTime
        )).thenReturn(List.of(purchase));

        List<StudentConsumptionPurchase> purchases =
                repository.findPurchasesByStudentIdAndDateRange(
                        studentId,
                        statuses,
                        startDateTime,
                        endDateTime
                );

        assertEquals(1, purchases.size());
        StudentConsumptionPurchase mapped = purchases.get(0);
        assertEquals(purchase.getId(), mapped.purchaseId());
        assertEquals(purchase.getDateAndTime(), mapped.dateTime());
        assertEquals(PurchaseState.ENTREGADO, mapped.status());
        assertEquals(PaymentMethod.CREDITOS, mapped.paymentMethod());
        assertEquals(new BigDecimal("1500.00"), mapped.totalAmount());
        assertEquals(purchase.getBuffetEntity().getId(), mapped.buffetId());
        assertEquals("Buffet Primario", mapped.buffetName());
        assertEquals("Agua", mapped.products().get(0).productName());
        assertEquals("Sandwich", mapped.products().get(1).productName());
        assertEquals("Bebidas", mapped.products().get(0).categoryName());
        assertEquals(new BigDecimal("300.00"), mapped.products().get(0).unitPrice());
        assertEquals(new BigDecimal("300.00"), mapped.products().get(0).subtotal());
        verify(repository).findPurchaseEntitiesByStudentIdAndDateRange(
                studentId,
                statuses,
                startDateTime,
                endDateTime
        );
    }

    private PresentialPurchaseEntity purchase() {
        PresentialPurchaseEntity purchase = new PresentialPurchaseEntity();
        purchase.setId(UUID.randomUUID());
        purchase.setDateAndTime(LocalDateTime.of(2026, 5, 20, 10, 30));
        purchase.setPurchaseState(PurchaseState.ENTREGADO);
        purchase.setPaymentMethod(PaymentMethod.CREDITOS);
        purchase.setTotalAmount(new BigDecimal("1500.00"));
        purchase.setBuffetEntity(buffet());
        purchase.addDetalle(detail("Sandwich", "Comida", "1200.00"));
        purchase.addDetalle(detail("Agua", "Bebidas", "300.00"));
        return purchase;
    }

    private BuffetEntity buffet() {
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());
        buffet.setName("Buffet Primario");
        return buffet;
    }

    private PurchaseDetailEntity detail(
            String productName,
            String categoryName,
            String amount
    ) {
        PurchaseDetailEntity detail = new PurchaseDetailEntity();
        detail.setId(UUID.randomUUID());
        detail.setProductEntity(product(productName, categoryName));
        detail.setAmount(1);
        detail.setUnitPrice(new BigDecimal(amount));
        detail.setSubtotal(new BigDecimal(amount));
        return detail;
    }

    private ProductEntity product(String name, String categoryName) {
        ProductEntity product = new ProductEntity();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setCategory(category(categoryName));
        return product;
    }

    private ProductCategoryEntity category(String name) {
        ProductCategoryEntity category = new ProductCategoryEntity();
        category.setId(UUID.randomUUID());
        category.setDescription(name);
        return category;
    }
}
