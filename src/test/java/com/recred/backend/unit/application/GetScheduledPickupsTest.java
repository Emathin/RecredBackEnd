package com.recred.backend.unit.application;

import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetScheduledPickupsRequest;
import com.recred.backend.application.GetScheduledPickupsResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetScheduledPickupsTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    private GetScheduledPickups useCase;
    private UUID buffetId;
    private LocalDate pickupDate;
    private EstadoRetiro withdrawalStatus;
    private EstadoCompra purchaseStatus;
    private UUID pickupSlotId;
    private UUID studentId;
    private UUID productId;
    private UUID purchaseId;
    private String search;

    @BeforeEach
    void setUp() {
        useCase = new GetScheduledPickups(purchaseRepository);
    }

    @Test
    void givenScheduledPickupsExist_whenGettingPickups_thenReturnsPickups() {
        givenScheduledPickupFilters();
        givenScheduledPickupsExist();

        List<GetScheduledPickupsResponse> response = whenGetScheduledPickups();

        thenScheduledPickupWasReturned(response);
        thenScheduledPickupsWereRequested();
    }

    @Test
    void givenNoScheduledPickupsExist_whenGettingPickups_thenReturnsEmptyList() {
        givenScheduledPickupFilters();
        givenNoScheduledPickupsExist();

        List<GetScheduledPickupsResponse> response = whenGetScheduledPickups();

        thenScheduledPickupsWereEmpty(response);
    }

    @Test
    void givenScheduledPickupHasStudentAndItems_whenGettingPickups_thenMapsFullDetails() {
        givenDetailedScheduledPickupFilters();
        givenScheduledPickupHasStudentAndItems();

        List<GetScheduledPickupsResponse> response = whenGetScheduledPickups();

        thenScheduledPickupDetailsWereReturned(response);
    }

    private void givenScheduledPickupFilters() {
        buffetId = randomUUID();
        pickupDate = LocalDate.now();
        withdrawalStatus = EstadoRetiro.PROGRAMADO;
        purchaseStatus = EstadoCompra.PENDIENTE;
        pickupSlotId = randomUUID();
        search = "test";
    }

    private void givenDetailedScheduledPickupFilters() {
        buffetId = randomUUID();
        pickupDate = LocalDate.of(2026, 7, 8);
        withdrawalStatus = EstadoRetiro.EN_PREPARACION;
        purchaseStatus = EstadoCompra.EN_PREPARACION;
        pickupSlotId = randomUUID();
        search = "Ana";
    }

    private void givenScheduledPickupsExist() {
        purchaseId = randomUUID();
        whenFindScheduledPickupsReturns(List.of(purchase(purchaseId)));
    }

    private void givenNoScheduledPickupsExist() {
        whenFindScheduledPickupsReturns(List.of());
    }

    private void givenScheduledPickupHasStudentAndItems() {
        purchaseId = randomUUID();
        studentId = randomUUID();
        productId = randomUUID();
        whenFindScheduledPickupsReturns(List.of(detailedPurchase()));
    }

    private List<GetScheduledPickupsResponse> whenGetScheduledPickups() {
        return useCase.execute(new GetScheduledPickupsRequest(
                buffetId,
                pickupDate,
                withdrawalStatus,
                purchaseStatus,
                pickupSlotId,
                search
        ));
    }

    private void thenScheduledPickupWasReturned(List<GetScheduledPickupsResponse> response) {
        assertEquals(1, response.size());
        assertEquals(purchaseId, response.get(0).id());
    }

    private void thenScheduledPickupsWereEmpty(List<GetScheduledPickupsResponse> response) {
        assertEquals(List.of(), response);
    }

    private void thenScheduledPickupDetailsWereReturned(
            List<GetScheduledPickupsResponse> response
    ) {
        GetScheduledPickupsResponse pickup = response.get(0);
        GetScheduledPickupsResponse.PurchaseItemResponse item = pickup.items().get(0);

        assertEquals(studentId, pickup.studentId());
        assertEquals("Ana Perez", pickup.studentName());
        assertEquals("EN_PREPARACION", pickup.status());
        assertEquals("En preparacion", pickup.statusLabel());
        assertEquals("EN_PREPARACION", pickup.withdrawalStatus());
        assertEquals("CREDITOS", pickup.paymentMethod());
        assertEquals("S111", pickup.withdrawalCode());
        assertEquals(pickupSlotId, pickup.pickupSlotId());
        assertEquals("Segundo recreo", pickup.pickupSlotDescription());
        assertEquals(pickupDate, pickup.pickupDate());
        assertEquals(productId, item.productId());
        assertEquals("Muffin", item.name());
        assertEquals(2, item.quantity());
        assertEquals(new BigDecimal("900.00"), item.price());
    }

    private void thenScheduledPickupsWereRequested() {
        verify(purchaseRepository).findScheduledPickups(
                buffetId,
                pickupDate,
                withdrawalStatus,
                purchaseStatus,
                pickupSlotId,
                search
        );
    }

    private void whenFindScheduledPickupsReturns(List<Purchase> purchases) {
        when(purchaseRepository.findScheduledPickups(
                buffetId,
                pickupDate,
                withdrawalStatus,
                purchaseStatus,
                pickupSlotId,
                search
        )).thenReturn(purchases);
    }

    private Purchase detailedPurchase() {
        Purchase purchase = purchase(purchaseId);
        purchase.setStudent(student());
        purchase.setTotalAmount(new BigDecimal("1800.00"));
        purchase.setStatus(EstadoCompra.EN_PREPARACION);
        purchase.setWithdrawalStatus(EstadoRetiro.EN_PREPARACION);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 12, 30));
        purchase.setWithdrawalCode("S111");
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupSlotDescription("Segundo recreo");
        purchase.setPickupDate(pickupDate);
        purchase.setItems(Map.of(product(), 2));
        return purchase;
    }

    private Purchase purchase(UUID requestedPurchaseId) {
        Purchase purchase = new Purchase();
        purchase.setId(requestedPurchaseId);
        return purchase;
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setNombre("Ana");
        student.setApellido("Perez");
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Muffin");
        product.setPrecio(new BigDecimal("900.00"));
        return product;
    }
}
