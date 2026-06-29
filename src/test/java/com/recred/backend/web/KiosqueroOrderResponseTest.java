package com.recred.backend.web;

import com.recred.backend.application.ListInventoryProductsResponse;
import com.recred.backend.application.ListKiosqueroOrdersResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.web.KiosqueroOrderResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KiosqueroOrderResponseTest {

    @Test
    void fromDomain_ShouldMapFullPurchase() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Product 1");
        product.setPrecio(new BigDecimal("10.00"));
        product.setRequierePreparacion(true);
        product.setCupoMaximoDiario(50);

        Grado grade = new Grado();
        grade.setAnio("1");
        grade.setDivision("A");

        Usuario user = new Usuario();
        user.setNombre("John");
        user.setApellido("Doe");

        Alumno student = new Alumno();
        student.setId(studentId);
        student.setUsuario(user);
        student.setGrado(grade);

        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setBuffetId(UUID.randomUUID());
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("20.00"));
        purchase.setDate(now);
        purchase.setWithdrawalCode("1234");
        purchase.setPickupDate(today);
        purchase.setPickupSlotId(UUID.randomUUID());
        purchase.setPickupSlotDescription("Recreo 1");
        purchase.setPickupSlotStart(LocalTime.of(10, 0));
        purchase.setPickupSlotEnd(LocalTime.of(10, 15));
        purchase.setStudent(student);
        purchase.setBuyerId(studentId);
        purchase.setBuyerType(BuyerType.STUDENT);
        purchase.setItems(Map.of(product, 2));

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(purchase);

        assertEquals(purchaseId, response.id());
        assertEquals(TipoCompra.ANTICIPADA, response.tipo());
        assertEquals(EstadoCompra.PENDIENTE, response.status());
        assertEquals(EstadoCompra.PENDIENTE.getLabel(), response.statusLabel());
        assertEquals(new BigDecimal("20.00"), response.totalAmount());
        assertEquals(now, response.date());
        assertEquals("1234", response.withdrawalCode());
        assertEquals(today, response.pickupDate());
        assertNotNull(response.pickupSlot());
        assertEquals("Recreo 1", response.pickupSlot().description());
        assertEquals(studentId, response.student().id());
        assertEquals("John", response.student().firstName());
        assertEquals("1", response.student().gradeYear());
        assertEquals(studentId, response.buyer().id());
        assertEquals("STUDENT", response.buyer().type());
        assertEquals(1, response.items().size());
        assertEquals("Product 1", response.items().get(0).productName());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("10.00"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("20.00"), response.items().get(0).subtotal());
        assertTrue(response.items().get(0).requierePreparacion());
        assertEquals(50, response.items().get(0).cupoMaximoDiario());
    }

    @Test
    void fromApplication_ShouldMapOrderResponse() {
        UUID productId = UUID.randomUUID();
        UUID inventoryProductId = productId;
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        ListKiosqueroOrdersResponse.OrderResponse order =
                new ListKiosqueroOrdersResponse.OrderResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "ANTICIPADA",
                        "LISTO",
                        "Listo para retirar",
                        new BigDecimal("20.00"),
                        "CREDITOS",
                        now,
                        "1234",
                        today,
                        new ListKiosqueroOrdersResponse.PickupSlotResponse(
                                UUID.randomUUID(),
                                "Recreo 1",
                                LocalTime.of(10, 0),
                                LocalTime.of(10, 15)
                        ),
                        new ListKiosqueroOrdersResponse.StudentResponse(
                                UUID.randomUUID(),
                                "John",
                                "Doe",
                                "1",
                                "A"
                        ),
                        new ListKiosqueroOrdersResponse.BuyerResponse(
                                UUID.randomUUID(),
                                "STUDENT"
                        ),
                        List.of(new ListKiosqueroOrdersResponse.ItemResponse(
                                productId,
                                "Product 1",
                                2,
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                true,
                                50
                        ))
                );
        Producto inventoryProduct = new Producto();
        inventoryProduct.setId(inventoryProductId);
        inventoryProduct.setNombre("Inventory Product");
        inventoryProduct.setPrecio(new BigDecimal("12.00"));
        ListInventoryProductsResponse.InventoryProductResponse inventoryResponse =
                ListInventoryProductsResponse.fromDomain(List.of(inventoryProduct))
                        .products()
                        .get(0);

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromApplication(
                order,
                Map.of(productId, inventoryResponse)
        );

        assertEquals(TipoCompra.ANTICIPADA, response.tipo());
        assertEquals(EstadoCompra.LISTO, response.status());
        assertEquals("Listo para retirar", response.statusLabel());
        assertEquals("Recreo 1", response.pickupSlot().description());
        assertEquals("John", response.student().firstName());
        assertEquals("STUDENT", response.buyer().type());
        assertEquals("Inventory Product", response.items().get(0).productName());
        assertEquals(new BigDecimal("12.00"), response.items().get(0).unitPrice());
    }

    @Test
    void fromDomain_ShouldHandleNullFields() {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStatus(null);
        purchase.setPaymentMethod(null);
        purchase.setPickupSlotId(null);
        purchase.setStudent(null);
        purchase.setBuyerType(null);
        purchase.setItems(null);

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(purchase);

        assertNull(response.statusLabel());
        assertNull(response.paymentMethod());
        assertNull(response.pickupSlot());
        assertNull(response.student());
        assertNull(response.buyer().type());
        assertEquals(0, response.items().size());
    }

    @Test
    void fromDomain_ShouldHandleStudentWithNullUserAndGrade() {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setUsuario(null);
        student.setGrado(null);

        Purchase purchase = new Purchase();
        purchase.setStudent(student);

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(purchase);

        assertNotNull(response.student());
        assertNull(response.student().firstName());
        assertNull(response.student().gradeYear());
    }

    @Test
    void fromDomain_ShouldUseInventoryProductsWhenProvided() {
        UUID productId = UUID.randomUUID();
        Producto domainProduct = new Producto();
        domainProduct.setId(productId);
        domainProduct.setNombre("Original");
        domainProduct.setPrecio(new BigDecimal("10.00"));

        Producto inventoryProduct = new Producto();
        inventoryProduct.setId(productId);
        inventoryProduct.setNombre("Inventory Name");
        inventoryProduct.setPrecio(new BigDecimal("12.00"));
        ListInventoryProductsResponse.InventoryProductResponse inventoryProductResponse =
                ListInventoryProductsResponse.fromDomain(List.of(inventoryProduct))
                        .products()
                        .get(0);

        Purchase purchase = new Purchase();
        purchase.setItems(Map.of(domainProduct, 1));

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(
                purchase,
                Map.of(productId, inventoryProductResponse)
        );

        assertEquals("Inventory Name", response.items().get(0).productName());
        assertEquals(new BigDecimal("12.00"), response.items().get(0).unitPrice());
    }

    @Test
    void fromDomain_ShouldHandleProductWithNullPrice() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setPrecio(null);

        Purchase purchase = new Purchase();
        purchase.setItems(Map.of(product, 1));

        KiosqueroOrderResponse response = KiosqueroOrderResponse.fromDomain(purchase);

        assertEquals(BigDecimal.ZERO, response.items().get(0).unitPrice());
        assertEquals(BigDecimal.ZERO, response.items().get(0).subtotal());
    }
}
