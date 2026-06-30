package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.infrastructure.web.PurchaseItemDTO;
import com.recred.backend.infrastructure.web.PurchaseResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PurchaseResponseDTOTest {

    @Test
    void constructor_WithCompletePurchase_ShouldMapAllFields() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.now();
        Producto product = new Producto();
        product.setId(productId);

        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student(studentId));
        purchase.setTotalAmount(new BigDecimal("15.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(date);
        purchase.setItems(Map.of(product, 3));

        PurchaseResponseDTO response = new PurchaseResponseDTO(purchase);

        assertEquals(purchaseId, response.getId());
        assertEquals(studentId, response.getStudentId());
        assertEquals(new BigDecimal("15.00"), response.getTotalAmount());
        assertEquals(EstadoCompra.PENDIENTE.name(), response.getStatus());
        assertEquals("A preparar", response.getStatusLabel());
        assertEquals(MedioPago.CREDITOS.name(), response.getPaymentMethod());
        assertEquals(date, response.getDate());
        assertEquals(productId, response.getItems().get(0).getProductId());
        assertEquals(3, response.getItems().get(0).getQuantity());
    }

    @Test
    void constructor_WithNullableFields_ShouldKeepNullableResponseFieldsNull() {
        Purchase purchase = new Purchase();
        purchase.setItems(null);

        PurchaseResponseDTO response = new PurchaseResponseDTO(purchase);

        assertNull(response.getStudentId());
        assertNull(response.getStatus());
        assertNull(response.getStatusLabel());
        assertNull(response.getPaymentMethod());
        assertNull(response.getItems());
    }

    @Test
    void dadoPurchaseCompleto_cuandoMapeaADTO_entoncesMapeaCorrectamenteLosItems() {

        Producto alfajor = new Producto();
        alfajor.setId(UUID.randomUUID());
        alfajor.setNombre("Alfajor Jorgito");
        alfajor.setPrecio(new BigDecimal("500.00"));

        Purchase compra = new Purchase();
        compra.setId(UUID.randomUUID());
        compra.setTotalAmount(new BigDecimal("1000.00"));
        compra.setStatus(EstadoCompra.ENTREGADO);
        compra.setPaymentMethod(MedioPago.CREDITOS);
        compra.setDate(LocalDateTime.now());
        compra.setItems(Map.of(alfajor, 2));


        PurchaseResponseDTO dto = new PurchaseResponseDTO(compra);

        assertEquals(compra.getId(), dto.getId());
        assertEquals(new BigDecimal("1000.00"), dto.getTotalAmount());
        assertEquals("ENTREGADO", dto.getStatus());
        assertNotNull(dto.getItems());
        assertEquals(1, dto.getItems().size());

        PurchaseItemDTO itemDto = dto.getItems().get(0);
        assertEquals(alfajor.getId(), itemDto.getProductId());
        assertEquals("Alfajor Jorgito", itemDto.getProductName());
        assertEquals(2, itemDto.getQuantity());
        assertEquals(new BigDecimal("500.00"), itemDto.getUnitPrice());
    }

    @Test
    void constructor_WithPromotionItems_ShouldGroupThem() {
        UUID promoId = UUID.randomUUID();
        String promoName = "Promo Coca+Alfajor";

        Producto product1 = new Producto();
        product1.setId(UUID.randomUUID());
        product1.setNombre("Coca-Cola");
        product1.setPrecio(new BigDecimal("300.00"));
        product1.setRequierePreparacion(false);
        product1.setPromocionId(promoId);
        product1.setPromocionNombre(promoName);

        Producto product2 = new Producto();
        product2.setId(UUID.randomUUID());
        product2.setNombre("Alfajor");
        product2.setPrecio(new BigDecimal("200.00"));
        product2.setRequierePreparacion(true);
        product2.setPromocionId(promoId);
        product2.setPromocionNombre(promoName);

        Producto standalone = new Producto();
        standalone.setId(UUID.randomUUID());
        standalone.setNombre("Chocolate");
        standalone.setPrecio(new BigDecimal("150.00"));
        standalone.setRequierePreparacion(false);

        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(new BigDecimal("650.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setItems(Map.of(
                product1, 1,
                product2, 1,
                standalone, 1
        ));

        PurchaseResponseDTO response = new PurchaseResponseDTO(purchase);

        assertEquals(2, response.getItems().size());

        // Find promo item
        PurchaseItemDTO promoItem = response.getItems().stream()
                .filter(item -> item.getProductId().equals(promoId))
                .findFirst()
                .orElseThrow();
        assertEquals(promoName, promoItem.getProductName());
        assertEquals(1, promoItem.getQuantity());
        assertEquals(new BigDecimal("500.00"), promoItem.getUnitPrice());
        assertEquals(true, promoItem.getRequierePreparacion());

        // Find standalone item
        PurchaseItemDTO standaloneItem = response.getItems().stream()
                .filter(item -> item.getProductId().equals(standalone.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Chocolate", standaloneItem.getProductName());
        assertEquals(1, standaloneItem.getQuantity());
        assertEquals(new BigDecimal("150.00"), standaloneItem.getUnitPrice());
        assertEquals(false, standaloneItem.getRequierePreparacion());
    }

    private Alumno student(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }
}

