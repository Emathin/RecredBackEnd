package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.application.GetPendingHistoryByStudentResponse;
import com.recred.backend.application.GetPresentialPurchaseResponse;
import com.recred.backend.infrastructure.web.PurchaseItemDTO;
import com.recred.backend.infrastructure.web.PurchaseResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        PurchaseItemDTO promoItem = response.getItems().stream()
                .filter(item -> item.getProductId().equals(promoId))
                .findFirst()
                .orElseThrow();
        assertEquals(promoName, promoItem.getProductName());
        assertEquals(1, promoItem.getQuantity());
        assertEquals(new BigDecimal("500.00"), promoItem.getUnitPrice());
        assertEquals(true, promoItem.getRequierePreparacion());

        PurchaseItemDTO standaloneItem = response.getItems().stream()
                .filter(item -> item.getProductId().equals(standaloneId))
                .findFirst()
                .orElseThrow();
        assertEquals("Chocolate", standaloneItem.getProductName());
        assertEquals(1, standaloneItem.getQuantity());
        assertEquals(new BigDecimal("150.00"), standaloneItem.getUnitPrice());
        assertEquals(false, standaloneItem.getRequierePreparacion());
    }

    @Test
    void constructor_WithPresentialPurchaseResponse_ShouldMapItemsAndPickupTime() {
        UUID productId = UUID.randomUUID();
        GetPresentialPurchaseResponse responseDto = new GetPresentialPurchaseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1800.00"),
                "PENDIENTE",
                "A preparar",
                "CREDITOS",
                LocalDateTime.now(),
                "ANTICIPADA",
                "RET-321",
                "Primer recreo",
                LocalDate.of(2026, 7, 2),
                LocalTime.of(9, 5),
                List.of(new GetPresentialPurchaseResponse.ItemResponse(
                        productId,
                        "Sandwich",
                        2,
                        new BigDecimal("900.00"),
                        true,
                        20,
                        null,
                        null
                ))
        );

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        assertEquals(responseDto.id(), response.getId());
        assertEquals("RET-321", response.getWithdrawalCode());
        assertEquals("Primer recreo", response.getPickupSlotDescription());
        assertEquals(LocalDate.of(2026, 7, 2), response.getPickupDate());
        assertEquals("09:05", response.getPickupSlotStartTime());
        assertEquals(1, response.getItems().size());
        assertEquals(productId, response.getItems().get(0).getProductId());
        assertEquals(true, response.getItems().get(0).getRequierePreparacion());
        assertEquals(20, response.getItems().get(0).getCupoMaximoDiario());
    }

    @Test
    void constructor_WithPendingHistory_ShouldGroupPromotionItemsAndFormatPickupTime() {
        UUID promoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID standaloneId = UUID.randomUUID();
        GetPendingHistoryByStudentResponse responseDto = new GetPendingHistoryByStudentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("2100.00"),
                "PENDIENTE",
                "A preparar",
                "CREDITOS",
                LocalDateTime.now(),
                List.of(
                        new GetPendingHistoryByStudentResponse.Item(
                                productId,
                                "Medialuna",
                                1,
                                new BigDecimal("600.00"),
                                false,
                                null,
                                promoId,
                                "Promo desayuno"
                        ),
                        new GetPendingHistoryByStudentResponse.Item(
                                UUID.randomUUID(),
                                "Cafe",
                                1,
                                new BigDecimal("800.00"),
                                true,
                                null,
                                promoId,
                                "Promo desayuno"
                        ),
                        new GetPendingHistoryByStudentResponse.Item(
                                standaloneId,
                                "Agua",
                                1,
                                new BigDecimal("700.00"),
                                false,
                                10,
                                null,
                                null
                        )
                ),
                "ANTICIPADA",
                "RET-654",
                "Segundo recreo",
                LocalDate.of(2026, 7, 3),
                LocalTime.of(10, 15),
                "PROGRAMADO"
        );

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        assertEquals("10:15", response.getPickupSlotStartTime());
        assertEquals(2, response.getItems().size());

        PurchaseItemDTO promoItem = response.getItems().stream()
                .filter(item -> item.getProductId().equals(promoId))
                .findFirst()
                .orElseThrow();
        assertEquals("Promo desayuno", promoItem.getProductName());
        assertEquals(new BigDecimal("1400.00"), promoItem.getUnitPrice());
        assertEquals(true, promoItem.getRequierePreparacion());

        PurchaseItemDTO standaloneItem = response.getItems().stream()
                .filter(item -> item.getProductId().equals(standaloneId))
                .findFirst()
                .orElseThrow();
        assertEquals("Agua", standaloneItem.getProductName());
        assertEquals(10, standaloneItem.getCupoMaximoDiario());
    }
}
