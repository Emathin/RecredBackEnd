package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.CloseDailyBuffetRequest;
import com.recred.backend.application.CloseDailyBuffetResponse;
import com.recred.backend.application.DailyBuffetCloseResponse;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GenerateDailyBuffetReportRequest;
import com.recred.backend.application.GenerateDailyBuffetReportResponse;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetDailyBuffetCloseStatusRequest;
import com.recred.backend.application.GetDailyBuffetCloseStatusResponse;
import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.application.GetKiosqueroDashboardRequest;
import com.recred.backend.application.GetKiosqueroDashboardResponse;
import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListDailyBuffetClosesRequest;
import com.recred.backend.application.ListDailyBuffetClosesResponse;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListInventoryProductsRequest;
import com.recred.backend.application.ListInventoryProductsResponse;
import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.ListKiosqueroOperationalAlertsResponse;
import com.recred.backend.application.ListKiosqueroOrdersRequest;
import com.recred.backend.application.ListKiosqueroOrdersResponse;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;

import com.recred.backend.domain.models.KiosqueroOperationalAlert;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.OperationalAlertSeverity;
import com.recred.backend.domain.models.OperationalAlertType;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import com.recred.backend.infrastructure.web.KiosqueroOperationalAlertResponse;
import com.recred.backend.infrastructure.web.KiosqueroOperationsController;
import com.recred.backend.infrastructure.web.KiosqueroOrderResponse;
import com.recred.backend.infrastructure.web.TimeSlotResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        CloseDailyBuffetRequest request = new CloseDailyBuffetRequest(buffetId, date);
        CloseDailyBuffetResponse result = new CloseDailyBuffetResponse(
                false,
                1,
                0,
                BigDecimal.ZERO,
                report(buffetId, date)
        );

        when(closeDailyBuffet.execute(request)).thenReturn(result);

        ResponseEntity<CloseDailyBuffetResponse> response =
                controller.closeDaily(buffetId, date, null);

        assertEquals(result, response.getBody());
        verify(closeDailyBuffet).execute(request);
    }

    @Test
    void dailyCloseStatus_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        GetDailyBuffetCloseStatusRequest request =
                new GetDailyBuffetCloseStatusRequest(buffetId, date);
        GetDailyBuffetCloseStatusResponse status = new GetDailyBuffetCloseStatusResponse(
                buffetId,
                date,
                true,
                2,
                4,
                BigDecimal.ZERO
        );

        when(getDailyBuffetCloseStatus.execute(request)).thenReturn(status);

        ResponseEntity<GetDailyBuffetCloseStatusResponse> response = controller.dailyCloseStatus(
                buffetId,
                date,
                null
        );

        assertEquals(status, response.getBody());
        verify(getDailyBuffetCloseStatus).execute(request);
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
        ListDailyBuffetClosesRequest request =
                new ListDailyBuffetClosesRequest(buffetId, from, to);
        ListDailyBuffetClosesResponse result =
                ListDailyBuffetClosesResponse.fromDomain(List.of(close));

        when(listDailyBuffetCloses.execute(request)).thenReturn(result);

        ResponseEntity<List<DailyBuffetCloseResponse>> response = controller.dailyCloses(
                buffetId,
                from,
                to,
                null
        );

        assertEquals(result.closes(), response.getBody());
        verify(listDailyBuffetCloses).execute(request);
    }

    @Test
    void dailyReport_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        GenerateDailyBuffetReportRequest request =
                new GenerateDailyBuffetReportRequest(buffetId, date);
        GenerateDailyBuffetReportResponse report = report(buffetId, date);

        when(generateDailyBuffetReport.execute(request)).thenReturn(report);

        ResponseEntity<GenerateDailyBuffetReportResponse> response =
                controller.dailyReport(buffetId, date, null);

        assertEquals(report, response.getBody());
        verify(generateDailyBuffetReport).execute(request);
    }

    @Test
    void dailyReportCsv_ShouldReturnCsvWithDownloadHeadersAndDefaultDate() {
        UUID buffetId = UUID.randomUUID();
        GenerateDailyBuffetReportResponse report = new GenerateDailyBuffetReportResponse(
                buffetId,
                LocalDate.of(2026, 6, 15),
                7,
                5,
                1,
                1,
                0,
                0,
                0,
                0,
                new BigDecimal("250.00"),
                BigDecimal.ZERO,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(generateDailyBuffetReport.execute(any(GenerateDailyBuffetReportRequest.class)))
                .thenReturn(report);

        ResponseEntity<String> response = controller.dailyReportCsv(buffetId, null, null);

        assertEquals(MediaType.valueOf("text/csv"), response.getHeaders().getContentType());
        assertEquals("attachment; filename=\"daily-report.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getBody().contains("metric,value"));
        assertTrue(response.getBody().contains("totalOrders,7"));
        assertTrue(response.getBody().contains("deliveredTotal,250.00"));
        verify(generateDailyBuffetReport).execute(any(GenerateDailyBuffetReportRequest.class));
    }

    @Test
    void dashboard_ShouldDelegateToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        GetKiosqueroDashboardRequest request =
                new GetKiosqueroDashboardRequest(buffetId, date, null, null);
        GetKiosqueroDashboardResponse dashboard = dashboard(buffetId, date);

        when(getKiosqueroDashboard.execute(request)).thenReturn(dashboard);

        ResponseEntity<GetKiosqueroDashboardResponse> response = controller.dashboard(
                buffetId,
                date,
                null,
                null,
                null
        );

        assertEquals(dashboard, response.getBody());
        verify(getKiosqueroDashboard).execute(request);
    }

    @Test
    void dashboard_ShouldDelegatePeriodToUseCase() {
        UUID buffetId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 14);
        GetKiosqueroDashboardRequest request =
                new GetKiosqueroDashboardRequest(buffetId, null, from, to);
        GetKiosqueroDashboardResponse dashboard = dashboard(buffetId, to);

        when(getKiosqueroDashboard.execute(request)).thenReturn(dashboard);

        ResponseEntity<GetKiosqueroDashboardResponse> response = controller.dashboard(
                buffetId,
                null,
                from,
                to,
                null
        );

        assertEquals(dashboard, response.getBody());
        verify(getKiosqueroDashboard).execute(request);
    }

    @Test
    void alerts_ShouldReturnMappedOperationalAlerts() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ListKiosqueroOperationalAlertsResponse.KiosqueroOperationalAlertDTO alert = new ListKiosqueroOperationalAlertsResponse.KiosqueroOperationalAlertDTO(
                buffetId,
                OperationalAlertType.DAILY_CAPACITY_LOW,
                OperationalAlertSeverity.WARNING,
                productId,
                null,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                "Cupo bajo"
        );
        ListKiosqueroOperationalAlertsResponse mockResponse = new ListKiosqueroOperationalAlertsResponse(List.of(alert));
        when(listKiosqueroOperationalAlerts.execute(buffetId)).thenReturn(mockResponse);

        ResponseEntity<ListKiosqueroOperationalAlertsResponse> response =
                controller.alerts(buffetId, null);

        ListKiosqueroOperationalAlertsResponse.KiosqueroOperationalAlertDTO body = response.getBody().alerts().get(0);
        assertEquals(buffetId, body.buffetId());
        assertEquals(OperationalAlertType.DAILY_CAPACITY_LOW, body.type());
        assertEquals(OperationalAlertSeverity.WARNING, body.severity());
        assertEquals(productId, body.productId());
        assertEquals("Cupo bajo", body.message());
        verify(listKiosqueroOperationalAlerts).execute(buffetId);
    }

    @Test
    void orders_ShouldReturnMappedOrders() {
        UUID buffetId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.of(2026, 6, 8);
        Purchase purchase = purchase(buffetId, pickupSlotId, pickupDate);
        ListKiosqueroOrdersRequest request = new ListKiosqueroOrdersRequest(
                buffetId,
                List.of(EstadoCompra.PENDIENTE),
                pickupDate,
                pickupSlotId,
                20
        );

        when(listKiosqueroOrders.execute(request))
                .thenReturn(ListKiosqueroOrdersResponse.from(List.of(purchase)));
        when(listInventoryProducts.execute(new ListInventoryProductsRequest(buffetId)))
                .thenReturn(new ListInventoryProductsResponse(List.of()));

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
        verify(listKiosqueroOrders).execute(request);
    }

    @Test
    void timeSlots_ShouldReturnMappedSlots() {
        UUID buffetId = UUID.randomUUID();
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        com.recred.backend.application.GetTimeSlotsBySchoolResponse slot = new com.recred.backend.application.GetTimeSlotsBySchoolResponse(
                slotId,
                colegioId,
                "Primer recreo",
                LocalTime.of(9, 30),
                LocalTime.of(9, 45),
                true,
                30,
                10
        );

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

    private GenerateDailyBuffetReportResponse report(UUID buffetId, LocalDate date) {
        return new GenerateDailyBuffetReportResponse(
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
                BigDecimal.ZERO,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private GetKiosqueroDashboardResponse dashboard(UUID buffetId, LocalDate date) {
        return new GetKiosqueroDashboardResponse(
                buffetId,
                date,
                new GetKiosqueroDashboardResponse.DashboardSummary(
                        BigDecimal.ZERO,
                        0,
                        0,
                        BigDecimal.ZERO,
                        0,
                        0
                ),
                new GetKiosqueroDashboardResponse.DashboardActivity(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                new GetKiosqueroDashboardResponse.DashboardProducts(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                new GetKiosqueroDashboardResponse.DashboardAlerts(
                        0,
                        0,
                        BigDecimal.ZERO,
                        0,
                        0,
                        0,
                        List.of()
                ),
                new GetKiosqueroDashboardResponse.DashboardTrends(List.of())
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

