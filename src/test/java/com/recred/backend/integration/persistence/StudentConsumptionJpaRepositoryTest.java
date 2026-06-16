package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.recred.backend.infrastructure.persistence.StudentConsumptionJpaRepository;
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
        List<EstadoCompra> statuses = List.of(EstadoCompra.ENTREGADO);
        LocalDateTime startDateTime = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2026, 5, 31, 23, 59);
        CompraPresencialEntity purchase = purchase();

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
        assertEquals(purchase.getFechaHora(), mapped.dateTime());
        assertEquals(EstadoCompra.ENTREGADO, mapped.status());
        assertEquals(MedioPago.CREDITOS, mapped.paymentMethod());
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

    private CompraPresencialEntity purchase() {
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setId(UUID.randomUUID());
        purchase.setFechaHora(LocalDateTime.of(2026, 5, 20, 10, 30));
        purchase.setEstado(EstadoCompra.ENTREGADO);
        purchase.setMedioPago(MedioPago.CREDITOS);
        purchase.setMontoTotal(new BigDecimal("1500.00"));
        purchase.setBuffetEntity(buffet());
        purchase.addDetalle(detail("Sandwich", "Comida", "1200.00"));
        purchase.addDetalle(detail("Agua", "Bebidas", "300.00"));
        return purchase;
    }

    private BuffetEntity buffet() {
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());
        buffet.setNombre("Buffet Primario");
        return buffet;
    }

    private CompraDetalleEntity detail(
            String productName,
            String categoryName,
            String amount
    ) {
        CompraDetalleEntity detail = new CompraDetalleEntity();
        detail.setId(UUID.randomUUID());
        detail.setProductoEntity(product(productName, categoryName));
        detail.setCantidad(1);
        detail.setPrecioUnitario(new BigDecimal(amount));
        detail.setSubtotal(new BigDecimal(amount));
        return detail;
    }

    private ProductoEntity product(String name, String categoryName) {
        ProductoEntity product = new ProductoEntity();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setCategoria(category(categoryName));
        return product;
    }

    private CategoriaProductoEntity category(String name) {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setId(UUID.randomUUID());
        category.setDescripcion(name);
        return category;
    }
}
