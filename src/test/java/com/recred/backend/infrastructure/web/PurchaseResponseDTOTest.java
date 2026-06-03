package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
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
        // Arrange
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
        compra.setItems(Map.of(alfajor, 2)); // 2 alfajores

        // Act
        PurchaseResponseDTO dto = new PurchaseResponseDTO(compra);

        // Assert
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

    private Alumno student(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }
}
