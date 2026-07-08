package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPendingHistoryByStudentTest {

    private static final UUID ALUMNO_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID PROMOTION_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("1200.00");
    private static final String PRODUCT_NAME = "Sandwich";
    private static final String PROMOTION_NAME = "Promo sandwich";
    private static final String PICKUP_SLOT = "Primer recreo";
    private static final String WITHDRAWAL_CODE = "P456";

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetPendingHistoryByStudent useCase;

    @Test
    void executeShouldReturnPendingPurchasesFromRepository() {
        Purchase purchase = buildPurchase();
        givenPendingPurchasesFound(List.of(purchase));

        List<GetPendingHistoryByStudentResponse> result = whenExecute();

        thenResultContainsOnePendingPurchase(result);
    }

    @Test
    void executeShouldReturnEmptyListWhenNoPendingPurchasesExist() {
        givenPendingPurchasesFound(List.of());

        List<GetPendingHistoryByStudentResponse> result = whenExecute();

        thenResultIsEmpty(result);
    }

    @Test
    void executeShouldMapPendingPurchaseFieldsAndItems() {
        Purchase purchase = buildPurchase();
        givenPendingPurchasesFound(List.of(purchase));

        List<GetPendingHistoryByStudentResponse> result = whenExecute();

        thenPurchaseAndItemAreMappedCorrectly(result);
    }

    private Purchase buildPurchase() {
        Producto product = buildProduct();
        Alumno alumno = new Alumno();
        alumno.setId(ALUMNO_ID);
        
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setStudent(alumno);
        purchase.setTotalAmount(AMOUNT);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 10, 0));
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode(WITHDRAWAL_CODE);
        purchase.setPickupSlotDescription(PICKUP_SLOT);
        purchase.setPickupDate(LocalDate.of(2026, 7, 7));
        purchase.setPickupSlotStartTime(LocalTime.of(9, 30));
        purchase.setWithdrawalStatus(EstadoRetiro.PROGRAMADO);
        purchase.setItems(Map.of(product, 1));
        
        return purchase;
    }

    private Producto buildProduct() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setNombre(PRODUCT_NAME);
        product.setPrecio(AMOUNT);
        product.setRequierePreparacion(false);
        product.setCupoMaximoDiario(15);
        product.setPromocionId(PROMOTION_ID);
        product.setPromocionNombre(PROMOTION_NAME);
        return product;
    }

    private void givenPendingPurchasesFound(List<Purchase> purchases) {
        when(purchaseRepository.findPendientesByAlumno(ALUMNO_ID)).thenReturn(purchases);
    }

    private List<GetPendingHistoryByStudentResponse> whenExecute() {
        return useCase.execute(new GetPendingHistoryByStudentRequest(ALUMNO_ID));
    }

    private void thenResultContainsOnePendingPurchase(List<GetPendingHistoryByStudentResponse> result) {
        assertEquals(1, result.size());
        assertEquals(PURCHASE_ID, result.get(0).id());
        assertEquals(AMOUNT, result.get(0).totalAmount());
    }

    private void thenResultIsEmpty(List<GetPendingHistoryByStudentResponse> result) {
        assertEquals(0, result.size());
    }

    private void thenPurchaseAndItemAreMappedCorrectly(List<GetPendingHistoryByStudentResponse> result) {
        GetPendingHistoryByStudentResponse response = result.get(0);
        GetPendingHistoryByStudentResponse.Item item = response.items().get(0);
        
        assertEquals(ALUMNO_ID, response.studentId());
        assertEquals("PENDIENTE", response.status());
        assertEquals("A preparar", response.statusLabel());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("ANTICIPADA", response.tipo());
        assertEquals(WITHDRAWAL_CODE, response.withdrawalCode());
        assertEquals(PICKUP_SLOT, response.pickupSlotDescription());
        assertEquals(LocalDate.of(2026, 7, 7), response.pickupDate());
        assertEquals(LocalTime.of(9, 30), response.pickupSlotStartTime());
        assertEquals("PROGRAMADO", response.withdrawalStatus());
        
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(PRODUCT_NAME, item.nombre());
        assertEquals(1, item.quantity());
        assertEquals(AMOUNT, item.precio());
        assertEquals(false, item.requierePreparacion());
        assertEquals(15, item.cupoMaximoDiario());
        assertEquals(PROMOTION_ID, item.promocionId());
        assertEquals(PROMOTION_NAME, item.promocionNombre());
    }
}
