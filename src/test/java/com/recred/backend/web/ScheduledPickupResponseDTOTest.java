package com.recred.backend.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.web.ScheduledPickupResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledPickupResponseDTOTest {

    @Test
    void constructor_ShouldMapFullPurchase() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Product 1");
        product.setPrecio(new BigDecimal("10.00"));

        Usuario user = new Usuario();
        user.setNombre("John");
        user.setApellido("Doe");

        Alumno student = new Alumno();
        student.setId(studentId);
        student.setNombre("John");
        student.setApellido("Doe");
        student.setUsuario(user);

        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setTotalAmount(new BigDecimal("20.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setWithdrawalStatus(EstadoRetiro.PROGRAMADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(now);
        purchase.setWithdrawalCode("1234");
        purchase.setPickupSlotId(UUID.randomUUID());
        purchase.setPickupSlotDescription("Recreo 1");
        purchase.setPickupDate(today);
        purchase.setItems(Map.of(product, 2));

        ScheduledPickupResponseDTO dto = new ScheduledPickupResponseDTO(purchase);

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
        assertNotNull(dto.getPickupSlotId());
        assertEquals("Recreo 1", dto.getPickupSlotDescription());
        assertEquals(today, dto.getPickupDate());
        assertEquals(1, dto.getItems().size());
        assertEquals("Product 1", dto.getItems().get(0).getProductName());
    }

    @Test
    void constructor_ShouldHandleNullFields() {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStudent(null);
        purchase.setStatus(null);
        purchase.setWithdrawalStatus(null);
        purchase.setPaymentMethod(null);
        purchase.setItems(null);

        ScheduledPickupResponseDTO dto = new ScheduledPickupResponseDTO(purchase);

        assertNull(dto.getStudentId());
        assertNull(dto.getStatus());
        assertNull(dto.getWithdrawalStatus());
        assertNull(dto.getPaymentMethod());
        assertNull(dto.getItems());
    }
}
