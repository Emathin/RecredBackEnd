package com.recred.backend.unit.infrastructure.web;

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

    private final UUID PURCHASE_ID = UUID.randomUUID();
    private final UUID STUDENT_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final LocalDateTime NOW = LocalDateTime.now();
    private final LocalDate TODAY = LocalDate.now();
    private final UUID SLOT_ID = UUID.randomUUID();

    @Test
    void fromDomainShouldMapFullPurchase() {
        Purchase purchase = givenFullPurchase();
        
        KiosqueroOrderResponse response = whenMappingFromDomain(purchase);
        
        thenFullPurchaseIsMapped(response);
    }

    @Test
    void fromApplicationShouldMapOrderResponse() {
        ListKiosqueroOrdersResponse.OrderResponse order = givenApplicationOrder();
        Map<UUID, ListInventoryProductsResponse.InventoryProductResponse> inventory = givenInventoryMap();
        
        KiosqueroOrderResponse response = whenMappingFromApplication(order, inventory);
        
        thenApplicationOrderIsMapped(response);
    }

    @Test
    void fromDomainShouldHandleNullFields() {
        Purchase purchase = givenPurchaseWithNullFields();
        
        KiosqueroOrderResponse response = whenMappingFromDomain(purchase);
        
        thenNullFieldsAreHandled(response);
    }

    @Test
    void fromDomainShouldHandleStudentWithNullUserAndGrade() {
        Purchase purchase = givenPurchaseWithNullStudentFields();
        
        KiosqueroOrderResponse response = whenMappingFromDomain(purchase);
        
        thenStudentNullFieldsAreHandled(response);
    }

    @Test
    void fromDomainShouldUseInventoryProductsWhenProvided() {
        Purchase purchase = givenPurchaseForInventory();
        Map<UUID, ListInventoryProductsResponse.InventoryProductResponse> inventory = givenInventoryMap();
        
        KiosqueroOrderResponse response = whenMappingFromDomain(purchase, inventory);
        
        thenInventoryProductsAreUsed(response);
    }

    @Test
    void fromDomainShouldHandleProductWithNullPrice() {
        Purchase purchase = givenPurchaseWithNullPriceProduct();
        
        KiosqueroOrderResponse response = whenMappingFromDomain(purchase);
        
        thenNullPriceIsHandled(response);
    }

    private Purchase givenFullPurchase() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
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
        student.setId(STUDENT_ID);
        student.setUsuario(user);
        student.setGrado(grade);

        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setBuffetId(UUID.randomUUID());
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("20.00"));
        purchase.setDate(NOW);
        purchase.setWithdrawalCode("1234");
        purchase.setPickupDate(TODAY);
        purchase.setPickupSlotId(SLOT_ID);
        purchase.setPickupSlotDescription("Recreo 1");
        purchase.setPickupSlotStart(LocalTime.of(10, 0));
        purchase.setPickupSlotEnd(LocalTime.of(10, 15));
        purchase.setStudent(student);
        purchase.setBuyerId(STUDENT_ID);
        purchase.setBuyerType(BuyerType.STUDENT);
        purchase.setItems(Map.of(product, 2));
        
        return purchase;
    }

    private ListKiosqueroOrdersResponse.OrderResponse givenApplicationOrder() {
        return new ListKiosqueroOrdersResponse.OrderResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ANTICIPADA",
                "LISTO",
                "Listo para retirar",
                new BigDecimal("20.00"),
                "CREDITOS",
                NOW,
                "1234",
                TODAY,
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
                        PRODUCT_ID,
                        "Product 1",
                        2,
                        new BigDecimal("10.00"),
                        new BigDecimal("20.00"),
                        true,
                        50
                ))
        );
    }

    private Map<UUID, ListInventoryProductsResponse.InventoryProductResponse> givenInventoryMap() {
        Producto inventoryProduct = new Producto();
        inventoryProduct.setId(PRODUCT_ID);
        inventoryProduct.setNombre("Inventory Name");
        inventoryProduct.setPrecio(new BigDecimal("12.00"));
        ListInventoryProductsResponse.InventoryProductResponse inventoryResponse =
                ListInventoryProductsResponse.fromDomain(List.of(inventoryProduct))
                        .products()
                        .get(0);
        return Map.of(PRODUCT_ID, inventoryResponse);
    }

    private Purchase givenPurchaseWithNullFields() {
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setStatus(null);
        purchase.setPaymentMethod(null);
        purchase.setPickupSlotId(null);
        purchase.setStudent(null);
        purchase.setBuyerType(null);
        purchase.setItems(null);
        return purchase;
    }

    private Purchase givenPurchaseWithNullStudentFields() {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        student.setUsuario(null);
        student.setGrado(null);

        Purchase purchase = new Purchase();
        purchase.setStudent(student);
        return purchase;
    }

    private Purchase givenPurchaseForInventory() {
        Producto domainProduct = new Producto();
        domainProduct.setId(PRODUCT_ID);
        domainProduct.setNombre("Original");
        domainProduct.setPrecio(new BigDecimal("10.00"));

        Purchase purchase = new Purchase();
        purchase.setItems(Map.of(domainProduct, 1));
        return purchase;
    }

    private Purchase givenPurchaseWithNullPriceProduct() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setPrecio(null);

        Purchase purchase = new Purchase();
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }

    private KiosqueroOrderResponse whenMappingFromDomain(Purchase purchase) {
        return KiosqueroOrderResponse.fromDomain(purchase);
    }

    private KiosqueroOrderResponse whenMappingFromDomain(Purchase purchase, Map<UUID, ListInventoryProductsResponse.InventoryProductResponse> inventory) {
        return KiosqueroOrderResponse.fromDomain(purchase, inventory);
    }

    private KiosqueroOrderResponse whenMappingFromApplication(ListKiosqueroOrdersResponse.OrderResponse order, Map<UUID, ListInventoryProductsResponse.InventoryProductResponse> inventory) {
        return KiosqueroOrderResponse.fromApplication(order, inventory);
    }

    private void thenFullPurchaseIsMapped(KiosqueroOrderResponse response) {
        assertEquals(PURCHASE_ID, response.id());
        assertEquals(TipoCompra.ANTICIPADA, response.tipo());
        assertEquals(EstadoCompra.PENDIENTE, response.status());
        assertEquals(EstadoCompra.PENDIENTE.getLabel(), response.statusLabel());
        assertEquals(new BigDecimal("20.00"), response.totalAmount());
        assertEquals(NOW, response.date());
        assertEquals("1234", response.withdrawalCode());
        assertEquals(TODAY, response.pickupDate());
        assertNotNull(response.pickupSlot());
        assertEquals("Recreo 1", response.pickupSlot().description());
        assertEquals(STUDENT_ID, response.student().id());
        assertEquals("John", response.student().firstName());
        assertEquals("1", response.student().gradeYear());
        assertEquals(STUDENT_ID, response.buyer().id());
        assertEquals("STUDENT", response.buyer().type());
        assertEquals(1, response.items().size());
        assertEquals("Product 1", response.items().get(0).productName());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("10.00"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("20.00"), response.items().get(0).subtotal());
        assertTrue(response.items().get(0).requierePreparacion());
        assertEquals(50, response.items().get(0).cupoMaximoDiario());
    }

    private void thenApplicationOrderIsMapped(KiosqueroOrderResponse response) {
        assertEquals(TipoCompra.ANTICIPADA, response.tipo());
        assertEquals(EstadoCompra.LISTO, response.status());
        assertEquals("Listo para retirar", response.statusLabel());
        assertEquals("Recreo 1", response.pickupSlot().description());
        assertEquals("John", response.student().firstName());
        assertEquals("STUDENT", response.buyer().type());
        assertEquals("Inventory Name", response.items().get(0).productName());
        assertEquals(new BigDecimal("12.00"), response.items().get(0).unitPrice());
    }

    private void thenNullFieldsAreHandled(KiosqueroOrderResponse response) {
        assertNull(response.statusLabel());
        assertNull(response.paymentMethod());
        assertNull(response.pickupSlot());
        assertNull(response.student());
        assertNull(response.buyer().type());
        assertEquals(0, response.items().size());
    }

    private void thenStudentNullFieldsAreHandled(KiosqueroOrderResponse response) {
        assertNotNull(response.student());
        assertNull(response.student().firstName());
        assertNull(response.student().gradeYear());
    }

    private void thenInventoryProductsAreUsed(KiosqueroOrderResponse response) {
        assertEquals("Inventory Name", response.items().get(0).productName());
        assertEquals(new BigDecimal("12.00"), response.items().get(0).unitPrice());
    }

    private void thenNullPriceIsHandled(KiosqueroOrderResponse response) {
        assertEquals(BigDecimal.ZERO, response.items().get(0).unitPrice());
        assertEquals(BigDecimal.ZERO, response.items().get(0).subtotal());
    }
}
