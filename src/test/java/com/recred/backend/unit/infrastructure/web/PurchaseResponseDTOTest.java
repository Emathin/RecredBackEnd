package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.infrastructure.web.PurchaseItemDTO;
import com.recred.backend.infrastructure.web.PurchaseResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

        GetHistoryByStudentResponse responseDto = new GetHistoryByStudentResponse(
                purchaseId, studentId, new BigDecimal("15.00"), "PENDIENTE", "A preparar",
                "CREDITOS", date, List.of(new GetHistoryByStudentResponse.Item(
                        productId, "Producto 1", 3, new BigDecimal("5.00"),
                        false, 10, null, null
                )), "PRESENCIAL", null, null, null, null, null
        );

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        assertEquals(purchaseId, response.getId());
        assertEquals(studentId, response.getStudentId());
        assertEquals(new BigDecimal("15.00"), response.getTotalAmount());
        assertEquals("PENDIENTE", response.getStatus());
        assertEquals("A preparar", response.getStatusLabel());
        assertEquals("CREDITOS", response.getPaymentMethod());
        assertEquals(date, response.getDate());
        assertEquals(productId, response.getItems().get(0).getProductId());
        assertEquals(3, response.getItems().get(0).getQuantity());
    }

    @Test
    void constructor_WithNullableFields_ShouldKeepNullableResponseFieldsNull() {
        GetHistoryByStudentResponse responseDto = new GetHistoryByStudentResponse(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        assertNull(response.getStudentId());
        assertNull(response.getStatus());
        assertNull(response.getStatusLabel());
        assertNull(response.getPaymentMethod());
        assertNull(response.getItems());
    }

    @Test
    void dadoPurchaseCompleto_cuandoMapeaADTO_entoncesMapeaCorrectamenteLosItems() {
        UUID alfajorId = UUID.randomUUID();

        GetHistoryByStudentResponse responseDto = new GetHistoryByStudentResponse(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1000.00"),
                "ENTREGADO", "Entregado", "CREDITOS", LocalDateTime.now(),
                List.of(new GetHistoryByStudentResponse.Item(
                        alfajorId, "Alfajor Jorgito", 2, new BigDecimal("500.00"),
                        false, null, null, null
                )),
                "PRESENCIAL", null, null, null, null, null
        );

        PurchaseResponseDTO dto = new PurchaseResponseDTO(responseDto);

        assertEquals(responseDto.id(), dto.getId());
        assertEquals(new BigDecimal("1000.00"), dto.getTotalAmount());
        assertEquals("ENTREGADO", dto.getStatus());
        assertNotNull(dto.getItems());
        assertEquals(1, dto.getItems().size());

        PurchaseItemDTO itemDto = dto.getItems().get(0);
        assertEquals(alfajorId, itemDto.getProductId());
        assertEquals("Alfajor Jorgito", itemDto.getProductName());
        assertEquals(2, itemDto.getQuantity());
        assertEquals(new BigDecimal("500.00"), itemDto.getUnitPrice());
    }

    @Test
    void constructor_WithPromotionItems_ShouldGroupThem() {
        UUID promoId = UUID.randomUUID();
        String promoName = "Promo Coca+Alfajor";
        UUID product1Id = UUID.randomUUID();
        UUID product2Id = UUID.randomUUID();
        UUID standaloneId = UUID.randomUUID();

        GetHistoryByStudentResponse responseDto = new GetHistoryByStudentResponse(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("650.00"),
                "PENDIENTE", "A preparar", "CREDITOS", LocalDateTime.now(),
                List.of(
                        new GetHistoryByStudentResponse.Item(
                                product1Id, "Coca-Cola", 1, new BigDecimal("300.00"),
                                false, null, promoId, promoName
                        ),
                        new GetHistoryByStudentResponse.Item(
                                product2Id, "Alfajor", 1, new BigDecimal("200.00"),
                                true, null, promoId, promoName
                        ),
                        new GetHistoryByStudentResponse.Item(
                                standaloneId, "Chocolate", 1, new BigDecimal("150.00"),
                                false, null, null, null
                        )
                ),
                "PRESENCIAL", null, null, null, null, null
        );

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

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
                .filter(item -> item.getProductId().equals(standaloneId))
                .findFirst()
                .orElseThrow();
        assertEquals("Chocolate", standaloneItem.getProductName());
        assertEquals(1, standaloneItem.getQuantity());
        assertEquals(new BigDecimal("150.00"), standaloneItem.getUnitPrice());
        assertEquals(false, standaloneItem.getRequierePreparacion());
    }
}

