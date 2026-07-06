package com.recred.backend.unit.application;

import com.recred.backend.application.ListKiosqueroOrdersResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListKiosqueroOrdersResponseTest {

    @Test
    void fromShouldMapFullOrder() {
        UUID studentId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        Producto product = product(new BigDecimal("500.00"));
        Purchase purchase = purchase(student(studentId), product);
        purchase.setBuyerId(buyerId);
        purchase.setBuyerType(BuyerType.TUTOR);
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupSlotStart(LocalTime.of(10, 0));
        purchase.setPickupSlotEnd(LocalTime.of(10, 30));

        ListKiosqueroOrdersResponse.OrderResponse response =
                ListKiosqueroOrdersResponse.from(List.of(purchase)).orders().get(0);

        assertEquals(purchase.getId(), response.id());
        assertEquals("ANTICIPADA", response.type());
        assertEquals("PENDIENTE", response.status());
        assertEquals("A preparar", response.statusLabel());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals(pickupSlotId, response.pickupSlot().id());
        assertEquals(studentId, response.student().id());
        assertEquals("Ana", response.student().firstName());
        assertEquals("Perez", response.student().lastName());
        assertEquals("4", response.student().gradeYear());
        assertEquals("B", response.student().gradeDivision());
        assertEquals(buyerId, response.buyer().id());
        assertEquals("TUTOR", response.buyer().type());
        assertEquals(product.getId(), response.items().get(0).productId());
        assertEquals(new BigDecimal("1000.00"), response.items().get(0).subtotal());
    }

    @Test
    void fromShouldHandleMissingOptionalValues() {
        Producto product = product(null);
        Purchase purchase = purchase(null, product);
        purchase.setBuyerType(null);
        purchase.setItems(null);

        ListKiosqueroOrdersResponse.OrderResponse response =
                ListKiosqueroOrdersResponse.from(List.of(purchase)).orders().get(0);

        assertNull(response.pickupSlot());
        assertNull(response.student());
        assertNull(response.buyer().type());
        assertTrue(response.items().isEmpty());

        purchase.setItems(Map.of(product, 2));
        response = ListKiosqueroOrdersResponse.from(List.of(purchase)).orders().get(0);

        assertEquals(BigDecimal.ZERO, response.items().get(0).unitPrice());
        assertEquals(BigDecimal.ZERO, response.items().get(0).subtotal());
    }

    private Purchase purchase(Alumno student, Producto product) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setBuffetId(UUID.randomUUID());
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("1000.00"));
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 12, 0));
        purchase.setWithdrawalCode("ABC123");
        purchase.setPickupDate(LocalDate.of(2026, 7, 5));
        purchase.setStudent(student);
        purchase.setItems(Map.of(product, 2));
        return purchase;
    }

    private Alumno student(UUID studentId) {
        Usuario user = new Usuario();
        user.setNombre("Ana");
        user.setApellido("Perez");
        Grado grade = new Grado();
        grade.setAnio("4");
        grade.setDivision("B");
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setUsuario(user);
        student.setGrado(grade);
        return student;
    }

    private Producto product(BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Agua");
        product.setPrecio(price);
        product.setRequierePreparacion(false);
        product.setCupoMaximoDiario(20);
        return product;
    }
}
