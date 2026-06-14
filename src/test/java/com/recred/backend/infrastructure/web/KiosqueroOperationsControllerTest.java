package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyBuffetReport;
import com.recred.backend.domain.models.DailyCloseResult;
import com.recred.backend.domain.models.DailyCloseStatus;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.KiosqueroDashboard;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KiosqueroOperationsControllerTest {

    private final CloseDailyBuffet closeDailyBuffet = mock(CloseDailyBuffet.class);
    private final GetDailyBuffetCloseStatus getDailyBuffetCloseStatus =
            mock(GetDailyBuffetCloseStatus.class);
    private final ListDailyBuffetCloses listDailyBuffetCloses =
            mock(ListDailyBuffetCloses.class);
    private final GenerateDailyBuffetReport generateDailyBuffetReport =
            mock(GenerateDailyBuffetReport.class);
    private final UpdatePickupSlotRules updatePickupSlotRules =
            mock(UpdatePickupSlotRules.class);
    private final ListKiosqueroOrders listKiosqueroOrders = mock(ListKiosqueroOrders.class);
    private final GetBuffetTimeSlots getBuffetTimeSlots = mock(GetBuffetTimeSlots.class);
    private final ListInventoryProducts listInventoryProducts = mock(ListInventoryProducts.class);
    private final ListKiosqueroOperationalAlerts listKiosqueroOperationalAlerts =
            mock(ListKiosqueroOperationalAlerts.class);
    private final GetKiosqueroDashboard getKiosqueroDashboard =
            mock(GetKiosqueroDashboard.class);
    private final KiosqueroAccessGuard kiosqueroAccessGuard = mock(KiosqueroAccessGuard.class);
    private final KiosqueroOperationsController controller =
            new KiosqueroOperationsController(
                    closeDailyBuffet,
                    getDailyBuffetCloseStatus,
                    listDailyBuffetCloses,
                    generateDailyBuffetReport,
                    updatePickupSlotRules,
                    listKiosqueroOrders,
                    getBuffetTimeSlots,
                    listInventoryProducts,
                    listKiosqueroOperationalAlerts,
                    getKiosqueroDashboard,
                    kiosqueroAccessGuard
            );

    @Test
    void closeDaily_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        DailyCloseResult result = new DailyCloseResult(1, report(buffetId, date));

        when(closeDailyBuffet.execute(buffetId, date)).thenReturn(result);

        ResponseEntity<DailyCloseResult> response = controller.closeDaily(buffetId, date, null);

        assertEquals(result, response.getBody());
        verify(closeDailyBuffet).execute(buffetId, date);
    }

    @Test
    void dailyCloseStatus_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        DailyCloseStatus status = new DailyCloseStatus(
                buffetId,
                date,
                true,
                2,
                4,
                BigDecimal.ZERO
        );

        when(getDailyBuffetCloseStatus.execute(buffetId, date)).thenReturn(status);

        ResponseEntity<DailyCloseStatus> response = controller.dailyCloseStatus(
                buffetId,
                date,
                null
        );

        assertEquals(status, response.getBody());
        verify(getDailyBuffetCloseStatus).execute(buffetId, date);
    }

    @Test
    void dailyCloses_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                LocalDate.of(2026, 5, 27),
                2,
                4,
                BigDecimal.ZERO
        );

        when(listDailyBuffetCloses.execute(buffetId, from, to)).thenReturn(List.of(close));

        ResponseEntity<List<DailyBuffetClose>> response = controller.dailyCloses(
                buffetId,
                from,
                to,
                null
        );

        assertEquals(List.of(close), response.getBody());
        verify(listDailyBuffetCloses).execute(buffetId, from, to);
    }

    @Test
    void dailyReport_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        DailyBuffetReport report = report(buffetId, date);

        when(generateDailyBuffetReport.execute(buffetId, date)).thenReturn(report);

        ResponseEntity<DailyBuffetReport> response = controller.dailyReport(buffetId, date, null);

        assertEquals(report, response.getBody());
        verify(generateDailyBuffetReport).execute(buffetId, date);
    }

    @Test
    void dashboard_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        KiosqueroDashboard dashboard = dashboard(buffetId, date);

        when(getKiosqueroDashboard.execute(buffetId, date, null, null)).thenReturn(dashboard);

        ResponseEntity<KiosqueroDashboard> response = controller.dashboard(
                buffetId,
                date,
                null,
                null,
                null
        );

        assertEquals(dashboard, response.getBody());
        verify(getKiosqueroDashboard).execute(buffetId, date, null, null);
    }

    @Test
    void dashboard_ShouldDelegatePeriodToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 14);
        KiosqueroDashboard dashboard = dashboard(buffetId, to);

        when(getKiosqueroDashboard.execute(buffetId, null, from, to)).thenReturn(dashboard);

        ResponseEntity<KiosqueroDashboard> response = controller.dashboard(
                buffetId,
                null,
                from,
                to,
                null
        );

        assertEquals(dashboard, response.getBody());
        verify(getKiosqueroDashboard).execute(buffetId, null, from, to);
    }

    @Test
    void orders_ShouldReturnMappedOrders() {
        UUID buffetId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.of(2026, 6, 8);
        Purchase purchase = purchase(buffetId, pickupSlotId, pickupDate);

        when(listKiosqueroOrders.execute(
                buffetId,
                List.of(EstadoCompra.PENDIENTE),
                pickupDate,
                pickupSlotId,
                20
        )).thenReturn(List.of(purchase));
        when(listInventoryProducts.execute(buffetId)).thenReturn(List.of());

        ResponseEntity<List<KiosqueroOrderResponse>> response = controller.orders(
                buffetId,
                List.of(EstadoCompra.PENDIENTE),
                pickupDate,
                pickupSlotId,
                20,
                null
        );

        KiosqueroOrderResponse order = response.getBody().get(0);
        assertEquals(purchase.getId(), order.id());
        assertEquals(buffetId, order.buffetId());
        assertEquals(EstadoCompra.PENDIENTE, order.status());
        assertEquals(TipoCompra.ANTICIPADA, order.tipo());
        assertEquals(pickupSlotId, order.pickupSlot().id());
        assertEquals("Primer recreo", order.pickupSlot().description());
        assertEquals(1, order.items().size());
        verify(listKiosqueroOrders).execute(
                buffetId,
                List.of(EstadoCompra.PENDIENTE),
                pickupDate,
                pickupSlotId,
                20
        );
    }

    @Test
    void timeSlots_ShouldReturnMappedSlots() {
        UUID buffetId = UUID.randomUUID();
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        FranjaHoraria slot = new FranjaHoraria(
                slotId,
                colegioId,
                "Primer recreo",
                LocalTime.of(9, 30),
                LocalTime.of(9, 45),
                true
        );
        slot.setCupoMaximo(30);
        slot.setMinutosCorte(10);

        when(getBuffetTimeSlots.execute(buffetId)).thenReturn(List.of(slot));

        ResponseEntity<List<TimeSlotResponse>> response = controller.timeSlots(buffetId, null);

        TimeSlotResponse body = response.getBody().get(0);
        assertEquals(slotId, body.id());
        assertEquals(colegioId, body.colegioId());
        assertEquals("Primer recreo", body.descripcion());
        assertEquals(30, body.cupoMaximo());
        assertEquals(10, body.minutosCorte());
        verify(getBuffetTimeSlots).execute(buffetId);
    }

    @Test
    void updatePickupSlotRules_ShouldDelegateToUseCase() {
        UUID slotId = UUID.randomUUID();
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(slotId);
        slot.setDescripcion("Primer recreo");
        slot.setCupoMaximo(10);
        slot.setMinutosCorte(20);

        when(updatePickupSlotRules.execute(slotId, 10, 20)).thenReturn(slot);

        ResponseEntity<KiosqueroOperationsController.PickupSlotRulesResponse> response =
                controller.updatePickupSlotRules(
                        slotId,
                        new KiosqueroOperationsController.PickupSlotRulesRequest(10, 20)
                );

        assertEquals(10, response.getBody().cupoMaximo());
        assertEquals(20, response.getBody().minutosCorte());
        verify(updatePickupSlotRules).execute(slotId, 10, 20);
    }

    private DailyBuffetReport report(UUID buffetId, LocalDate date) {
        return new DailyBuffetReport(
                buffetId,
                date,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                List.of(),
                List.of()
        );
    }

    private KiosqueroDashboard dashboard(UUID buffetId, LocalDate date) {
        return new KiosqueroDashboard(
                buffetId,
                date,
                new KiosqueroDashboard.DashboardSummary(
                        BigDecimal.ZERO,
                        0,
                        0,
                        BigDecimal.ZERO,
                        0,
                        0
                ),
                new KiosqueroDashboard.DashboardActivity(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                new KiosqueroDashboard.DashboardProducts(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                new KiosqueroDashboard.DashboardAlerts(
                        0,
                        0,
                        BigDecimal.ZERO,
                        0,
                        0,
                        0,
                        List.of()
                ),
                new KiosqueroDashboard.DashboardTrends(List.of())
        );
    }

    private Purchase purchase(UUID buffetId, UUID pickupSlotId, LocalDate pickupDate) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("25.00"));

        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setBuffetId(buffetId);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setTotalAmount(new BigDecimal("25.00"));
        purchase.setDate(LocalDateTime.of(2026, 6, 8, 8, 30));
        purchase.setBuyerId(UUID.randomUUID());
        purchase.setBuyerType(BuyerType.TUTOR);
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupSlotStart(LocalTime.of(9, 30));
        purchase.setPickupSlotEnd(LocalTime.of(9, 45));
        purchase.setPickupDate(pickupDate);
        purchase.setWithdrawalCode("1234");
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }
}
