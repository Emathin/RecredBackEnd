package com.recred.backend.web;

import com.recred.backend.application.GetScheduledPickupsResponse;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.infrastructure.web.ScheduledPickupResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledPickupResponseDTOTest {

    @Test
    void constructor_ShouldMapFullPurchase() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        GetScheduledPickupsResponse response = new GetScheduledPickupsResponse(
                purchaseId,
                studentId,
                "John Doe",
                new BigDecimal("20.00"),
                "PENDIENTE",
                EstadoCompra.PENDIENTE.getLabel(),
                "PROGRAMADO",
                "CREDITOS",
                now,
                "1234",
                pickupSlotId,
                "Recreo 1",
                today,
                List.of(new GetScheduledPickupsResponse.PurchaseItemResponse(
                        productId,
                        "Product 1",
                        2,
                        new BigDecimal("10.00")
                ))
        );

        ScheduledPickupResponseDTO dto = new ScheduledPickupResponseDTO(response);

        assertEquals(purchaseId, dto.getId());
        assertEquals(studentId, dto.getStudentId());
        assertEquals("John Doe", dto.getStudentName());
        assertEquals(new BigDecimal("20.00"), dto.getTotalAmount());
        assertEquals("PENDIENTE", dto.getStatus());
        assertEquals(EstadoCompra.PENDIENTE.getLabel(), dto.getStatusLabel());
        assertEquals("PROGRAMADO", dto.getWithdrawalStatus());
        assertEquals("CREDITOS", dto.getPaymentMethod());
        assertEquals(now, dto.getDate());
        assertEquals("1234", dto.getWithdrawalCode());
        assertEquals(pickupSlotId, dto.getPickupSlotId());
        assertEquals("Recreo 1", dto.getPickupSlotDescription());
        assertEquals(today, dto.getPickupDate());
        assertEquals(1, dto.getItems().size());
        assertEquals("Product 1", dto.getItems().get(0).getProductName());
    }

    @Test
    void constructor_ShouldHandleNullFields() {
        GetScheduledPickupsResponse response = new GetScheduledPickupsResponse(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ScheduledPickupResponseDTO dto = new ScheduledPickupResponseDTO(response);

        assertNull(dto.getStudentId());
        assertNull(dto.getStatus());
        assertNull(dto.getWithdrawalStatus());
        assertNull(dto.getPaymentMethod());
        assertNull(dto.getItems());
    }
}
