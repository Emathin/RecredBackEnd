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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPendingHistoryByStudentTest {

    @Test
    void executeShouldReturnPendingPurchasesFromRepository() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        GetPendingHistoryByStudent useCase =
                new GetPendingHistoryByStudent(purchaseRepository);
        UUID alumnoId = UUID.randomUUID();
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(new BigDecimal("1200.00"));
        
        when(purchaseRepository.findPendientesByAlumno(alumnoId))
                .thenReturn(List.of(purchase));

        List<GetPendingHistoryByStudentResponse> result = useCase.execute(
                new GetPendingHistoryByStudentRequest(alumnoId)
        );

        assertEquals(1, result.size());
        assertEquals(purchase.getId(), result.get(0).id());
        assertEquals(purchase.getTotalAmount(), result.get(0).totalAmount());
    }

    @Test
    void executeShouldReturnEmptyListWhenNoPendingPurchasesExist() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        GetPendingHistoryByStudent useCase =
                new GetPendingHistoryByStudent(purchaseRepository);
        UUID alumnoId = UUID.randomUUID();

        when(purchaseRepository.findPendientesByAlumno(alumnoId))
                .thenReturn(List.of());

        List<GetPendingHistoryByStudentResponse> result = useCase.execute(
                new GetPendingHistoryByStudentRequest(alumnoId)
        );

        assertEquals(0, result.size());
    }

    @Test
    void executeShouldMapPendingPurchaseFieldsAndItems() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        GetPendingHistoryByStudent useCase =
                new GetPendingHistoryByStudent(purchaseRepository);
        UUID alumnoId = UUID.randomUUID();
        Producto product = product();
        Purchase purchase = purchase(alumnoId, product);

        when(purchaseRepository.findPendientesByAlumno(alumnoId))
                .thenReturn(List.of(purchase));

        List<GetPendingHistoryByStudentResponse> result = useCase.execute(
                new GetPendingHistoryByStudentRequest(alumnoId)
        );

        GetPendingHistoryByStudentResponse response = result.get(0);
        GetPendingHistoryByStudentResponse.Item item = response.items().get(0);
        assertEquals(alumnoId, response.studentId());
        assertEquals("PENDIENTE", response.status());
        assertEquals("A preparar", response.statusLabel());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("ANTICIPADA", response.tipo());
        assertEquals("P456", response.withdrawalCode());
        assertEquals("Primer recreo", response.pickupSlotDescription());
        assertEquals(LocalDate.of(2026, 7, 7), response.pickupDate());
        assertEquals(LocalTime.of(9, 30), response.pickupSlotStartTime());
        assertEquals("PROGRAMADO", response.withdrawalStatus());
        assertEquals(product.getId(), item.productId());
        assertEquals("Sandwich", item.nombre());
        assertEquals(1, item.quantity());
        assertEquals(new BigDecimal("1200.00"), item.precio());
        assertEquals(false, item.requierePreparacion());
        assertEquals(15, item.cupoMaximoDiario());
        assertEquals(product.getPromocionId(), item.promocionId());
        assertEquals("Promo sandwich", item.promocionNombre());
    }

    private Purchase purchase(UUID alumnoId, Producto product) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStudent(alumno);
        purchase.setTotalAmount(new BigDecimal("1200.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 10, 0));
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("P456");
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupDate(LocalDate.of(2026, 7, 7));
        purchase.setPickupSlotStartTime(LocalTime.of(9, 30));
        purchase.setWithdrawalStatus(EstadoRetiro.PROGRAMADO);
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Sandwich");
        product.setPrecio(new BigDecimal("1200.00"));
        product.setRequierePreparacion(false);
        product.setCupoMaximoDiario(15);
        product.setPromocionId(UUID.randomUUID());
        product.setPromocionNombre("Promo sandwich");
        return product;
    }
}
