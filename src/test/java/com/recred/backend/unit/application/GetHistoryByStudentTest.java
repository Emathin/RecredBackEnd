package com.recred.backend.unit.application;

import com.recred.backend.application.GetHistoryByStudent;
import com.recred.backend.application.GetHistoryByStudentRequest;
import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetHistoryByStudentTest {

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final GetHistoryByStudent useCase = new GetHistoryByStudent(purchaseRepository);

    private UUID studentId;
    private Producto product;
    private Purchase firstPurchase;
    private Purchase secondPurchase;
    private List<GetHistoryByStudentResponse> response;

    @Test
    void execute_WhenStudentHasPurchases_ReturnsPurchaseHistory() {
        givenStudentHasTwoPurchases();

        whenGettingStudentHistory();

        thenStudentPurchasesAreReturned(firstPurchase, secondPurchase);
        thenStudentHistoryWasRequested(studentId);
    }

    @Test
    void execute_WhenStudentHasNoPurchases_ReturnsEmptyHistory() {
        givenStudentHasNoPurchases();

        whenGettingStudentHistory();

        thenHistoryIsEmpty();
        thenStudentHistoryWasRequested(studentId);
    }

    @Test
    void execute_WhenPurchaseIsComplete_MapsPurchaseAndItems() {
        givenStudentHasCompletePurchase();

        whenGettingStudentHistory();

        thenCompletePurchaseIsMapped(
                studentId,
                "LISTO",
                "Listo para retirar",
                "CREDITOS",
                "ANTICIPADA"
        );
        thenPurchaseItemIsMapped(product, 2);
    }

    @Test
    void execute_WhenPurchaseHasNoOptionalData_MapsNullOptionalFields() {
        givenStudentHasPurchaseWithoutOptionalData();

        whenGettingStudentHistory();

        thenMissingPurchaseDataUsesDomainDefaults("PRESENCIAL");
    }

    private void givenStudentHasTwoPurchases() {
        studentId = randomUUID();
        firstPurchase = purchaseWithId(randomUUID());
        secondPurchase = purchaseWithId(randomUUID());
        givenPurchasesFound(firstPurchase, secondPurchase);
    }

    private void givenStudentHasNoPurchases() {
        studentId = randomUUID();
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of());
    }

    private void givenStudentHasCompletePurchase() {
        studentId = randomUUID();
        product = product();
        firstPurchase = completePurchase(studentId, product);
        givenPurchasesFound(firstPurchase);
    }

    private void givenStudentHasPurchaseWithoutOptionalData() {
        studentId = randomUUID();
        firstPurchase = purchaseWithId(randomUUID());
        givenPurchasesFound(firstPurchase);
    }

    private void givenPurchasesFound(Purchase... purchases) {
        when(purchaseRepository.findHistorialByAlumno(studentId))
                .thenReturn(List.of(purchases));
    }

    private void whenGettingStudentHistory() {
        response = useCase.execute(defaultRequest());
    }

    private void thenStudentPurchasesAreReturned(Purchase firstPurchase, Purchase secondPurchase) {
        assertEquals(2, response.size());
        assertEquals(firstPurchase.getId(), response.get(0).id());
        assertEquals(secondPurchase.getId(), response.get(1).id());
    }

    private void thenHistoryIsEmpty() {
        assertEquals(List.of(), response);
    }

    private void thenCompletePurchaseIsMapped(
            UUID expectedStudentId,
            String expectedStatus,
            String expectedStatusLabel,
            String expectedPaymentMethod,
            String expectedPurchaseType
    ) {
        GetHistoryByStudentResponse purchaseResponse = response.get(0);
        assertEquals(expectedStudentId, purchaseResponse.studentId());
        assertEquals(expectedStatus, purchaseResponse.status());
        assertEquals(expectedStatusLabel, purchaseResponse.statusLabel());
        assertEquals(expectedPaymentMethod, purchaseResponse.paymentMethod());
        assertEquals(expectedPurchaseType, purchaseResponse.tipo());
        assertEquals("R123", purchaseResponse.withdrawalCode());
        assertEquals("Recreo largo", purchaseResponse.pickupSlotDescription());
        assertEquals(LocalDate.of(2026, 7, 6), purchaseResponse.pickupDate());
        assertEquals(LocalTime.of(10, 30), purchaseResponse.pickupSlotStartTime());
        assertEquals("LISTO", purchaseResponse.withdrawalStatus());
    }

    private void thenPurchaseItemIsMapped(Producto expectedProduct, int expectedQuantity) {
        GetHistoryByStudentResponse.Item item = response.get(0).items().get(0);
        assertEquals(expectedProduct.getId(), item.productId());
        assertEquals("Agua", item.nombre());
        assertEquals(expectedQuantity, item.quantity());
        assertEquals(new BigDecimal("500.00"), item.precio());
        assertEquals(true, item.requierePreparacion());
        assertEquals(20, item.cupoMaximoDiario());
        assertEquals(expectedProduct.getPromocionId(), item.promocionId());
        assertEquals("Promo agua", item.promocionNombre());
    }

    private void thenMissingPurchaseDataUsesDomainDefaults(String expectedPurchaseType) {
        GetHistoryByStudentResponse purchaseResponse = response.get(0);
        assertNull(purchaseResponse.studentId());
        assertNull(purchaseResponse.status());
        assertNull(purchaseResponse.statusLabel());
        assertNull(purchaseResponse.paymentMethod());
        assertEquals(expectedPurchaseType, purchaseResponse.tipo());
        assertNull(purchaseResponse.withdrawalStatus());
        assertEquals(List.of(), purchaseResponse.items());
    }

    private void thenStudentHistoryWasRequested(UUID expectedStudentId) {
        verify(purchaseRepository).findHistorialByAlumno(expectedStudentId);
    }

    private GetHistoryByStudentRequest defaultRequest() {
        return new GetHistoryByStudentRequest(studentId);
    }

    private Purchase completePurchase(UUID studentId, Producto product) {
        Purchase purchase = purchaseWithId(randomUUID());
        purchase.setStudent(student(studentId));
        purchase.setTotalAmount(new BigDecimal("1000.00"));
        purchase.setStatus(EstadoCompra.LISTO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 9, 15));
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("R123");
        purchase.setPickupSlotDescription("Recreo largo");
        purchase.setPickupDate(LocalDate.of(2026, 7, 6));
        purchase.setPickupSlotStartTime(LocalTime.of(10, 30));
        purchase.setWithdrawalStatus(EstadoRetiro.LISTO);
        purchase.setItems(Map.of(product, 2));
        return purchase;
    }

    private Purchase purchaseWithId(UUID purchaseId) {
        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        return purchase;
    }

    private Alumno student(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre("Agua");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(true);
        product.setCupoMaximoDiario(20);
        product.setPromocionId(randomUUID());
        product.setPromocionNombre("Promo agua");
        return product;
    }
}
