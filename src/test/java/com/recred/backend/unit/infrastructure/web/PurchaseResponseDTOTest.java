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

    private static final UUID PURCHASE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROMO_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID STANDALONE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ALFAJOR_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void constructorWithCompletePurchaseShouldMapAllFields() {
        LocalDateTime date = LocalDateTime.now();
        GetHistoryByStudentResponse responseDto = givenHistoryResponse(PURCHASE_ID, STUDENT_ID, PRODUCT_ID, date);

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        thenMapsAllFieldsCorrectly(response, PURCHASE_ID, STUDENT_ID, PRODUCT_ID, date);
    }

    @Test
    void constructorWithNullableFieldsShouldKeepNullableResponseFieldsNull() {
        GetHistoryByStudentResponse responseDto = givenHistoryResponseWithNulls();

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        thenKeepsNullableFieldsNull(response);
    }

    @Test
    void constructorWithCompletePurchaseMapsItemsCorrectly() {
        GetHistoryByStudentResponse responseDto = givenHistoryResponseWithAlfajor(ALFAJOR_ID);

        PurchaseResponseDTO dto = new PurchaseResponseDTO(responseDto);

        thenMapsItemsCorrectly(dto, ALFAJOR_ID);
    }

    @Test
    void constructorWithPromotionItemsShouldGroupThem() {
        String promoName = "Promo Coca+Alfajor";
        GetHistoryByStudentResponse responseDto = givenHistoryResponseWithPromotions(PROMO_ID, promoName, STANDALONE_ID);

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        thenGroupsPromotionItemsCorrectly(response, PROMO_ID, promoName, STANDALONE_ID);
    }

    @Test
    void constructorWithPresentialPurchaseResponseShouldMapItemsAndPickupTime() {
        GetPresentialPurchaseResponse responseDto = givenPresentialPurchaseResponse(PRODUCT_ID);

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        thenMapsPresentialPurchaseCorrectly(response, responseDto, PRODUCT_ID);
    }

    @Test
    void constructorWithPendingHistoryShouldGroupPromotionItemsAndFormatPickupTime() {
        GetPendingHistoryByStudentResponse responseDto = givenPendingHistoryResponseWithPromotions(PROMO_ID, STANDALONE_ID);

        PurchaseResponseDTO response = new PurchaseResponseDTO(responseDto);

        thenGroupsPendingHistoryPromotionsAndFormatsTimeCorrectly(response, PROMO_ID, STANDALONE_ID);
    }

    private GetHistoryByStudentResponse givenHistoryResponse(
            UUID purchaseId,
            UUID studentId,
            UUID productId,
            LocalDateTime date
    ) {
        return new GetHistoryByStudentResponse(
                purchaseId, studentId, new BigDecimal("15.00"), "PENDIENTE", "A preparar",
                "CREDITOS", date, List.of(new GetHistoryByStudentResponse.Item(
                        productId, "Producto 1", 3, new BigDecimal("5.00"),
                        false, 10, null, null
                )), "PRESENCIAL", null, null, null, null, null, null
        );
    }

    private GetHistoryByStudentResponse givenHistoryResponseWithNulls() {
        return new GetHistoryByStudentResponse(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,null
        );
    }

    private GetHistoryByStudentResponse givenHistoryResponseWithAlfajor(UUID alfajorId) {
        return new GetHistoryByStudentResponse(
                PURCHASE_ID, STUDENT_ID, new BigDecimal("1000.00"),
                "ENTREGADO", "Entregado", "CREDITOS", LocalDateTime.now(),
                List.of(new GetHistoryByStudentResponse.Item(
                        alfajorId, "Alfajor Jorgito", 2, new BigDecimal("500.00"),
                        false, null, null, null
                )),
                "PRESENCIAL", null, null, null, null, null, null
        );
    }

    private GetHistoryByStudentResponse givenHistoryResponseWithPromotions(
            UUID promoId,
            String promoName,
            UUID standaloneId
    ) {
        return new GetHistoryByStudentResponse(
                PURCHASE_ID, STUDENT_ID, new BigDecimal("650.00"),
                "PENDIENTE", "A preparar", "CREDITOS", LocalDateTime.now(),
                List.of(
                        new GetHistoryByStudentResponse.Item(
                                UUID.randomUUID(), "Coca-Cola", 1, new BigDecimal("300.00"),
                                false, null, promoId, promoName
                        ),
                        new GetHistoryByStudentResponse.Item(
                                UUID.randomUUID(), "Alfajor", 1, new BigDecimal("200.00"),
                                true, null, promoId, promoName
                        ),
                        new GetHistoryByStudentResponse.Item(
                                standaloneId, "Chocolate", 1, new BigDecimal("150.00"),
                                false, null, null, null
                        )
                ),
                "PRESENCIAL", null, null, null, null, null, null
        );
    }

    private GetPresentialPurchaseResponse givenPresentialPurchaseResponse(UUID productId) {
        return new GetPresentialPurchaseResponse(
                PURCHASE_ID,
                STUDENT_ID,
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
    }

    private GetPendingHistoryByStudentResponse givenPendingHistoryResponseWithPromotions(
            UUID promoId,
            UUID standaloneId
    ) {
        return new GetPendingHistoryByStudentResponse(
                PURCHASE_ID,
                STUDENT_ID,
                new BigDecimal("2100.00"),
                "PENDIENTE",
                "A preparar",
                "CREDITOS",
                LocalDateTime.now(),
                List.of(
                        new GetPendingHistoryByStudentResponse.Item(
                                UUID.randomUUID(),
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
                "PROGRAMADO",
                null
        );
    }

    private void thenMapsAllFieldsCorrectly(
            PurchaseResponseDTO response,
            UUID purchaseId,
            UUID studentId,
            UUID productId,
            LocalDateTime date
    ) {
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

    private void thenKeepsNullableFieldsNull(PurchaseResponseDTO response) {
        assertNull(response.getStudentId());
        assertNull(response.getStatus());
        assertNull(response.getStatusLabel());
        assertNull(response.getPaymentMethod());
        assertNull(response.getItems());
    }

    private void thenMapsItemsCorrectly(PurchaseResponseDTO dto, UUID alfajorId) {
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

    private void thenGroupsPromotionItemsCorrectly(
            PurchaseResponseDTO response,
            UUID promoId,
            String promoName,
            UUID standaloneId
    ) {
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

    private void thenMapsPresentialPurchaseCorrectly(
            PurchaseResponseDTO response,
            GetPresentialPurchaseResponse responseDto,
            UUID productId
    ) {
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

    private void thenGroupsPendingHistoryPromotionsAndFormatsTimeCorrectly(
            PurchaseResponseDTO response,
            UUID promoId,
            UUID standaloneId
    ) {
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
