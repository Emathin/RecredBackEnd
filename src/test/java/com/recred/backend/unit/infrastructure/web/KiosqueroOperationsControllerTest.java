package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.CloseDailyBuffetRequest;
import com.recred.backend.application.CloseDailyBuffetResponse;
import com.recred.backend.application.DailyBuffetCloseResponse;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GenerateDailyBuffetReportRequest;
import com.recred.backend.application.GenerateDailyBuffetReportResponse;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetBuffetTimeSlotsRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
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
import com.recred.backend.application.ListKiosqueroOrdersRequest;
import com.recred.backend.application.ListKiosqueroOrdersResponse;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.ListKiosqueroOperationalAlertsRequest;
import com.recred.backend.application.ListKiosqueroOperationalAlertsResponse;
import com.recred.backend.application.OperationalAlertResponse;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.application.UpdatePickupSlotRulesRequest;
import com.recred.backend.application.UpdatePickupSlotRulesResponse;
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
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
import com.recred.backend.infrastructure.web.TimeSlotResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KiosqueroOperationsControllerTest {

    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID COLEGIO_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID CLOSE_ID = UUID.randomUUID();
    private static final LocalDate DEFAULT_DATE = LocalDate.of(2026, 5, 27);
    private static final LocalDate FROM_DATE = LocalDate.of(2026, 5, 1);
    private static final LocalDate TO_DATE = LocalDate.of(2026, 5, 31);
    private static final LocalDateTime ALERT_TIME = LocalDateTime.of(2026, 6, 15, 10, 0);

    @Mock private CloseDailyBuffet closeDailyBuffet;
    @Mock private GetDailyBuffetCloseStatus getDailyBuffetCloseStatus;
    @Mock private ListDailyBuffetCloses listDailyBuffetCloses;
    @Mock private GenerateDailyBuffetReport generateDailyBuffetReport;
    @Mock private UpdatePickupSlotRules updatePickupSlotRules;
    @Mock private ListKiosqueroOrders listKiosqueroOrders;
    @Mock private GetBuffetTimeSlots getBuffetTimeSlots;
    @Mock private ListInventoryProducts listInventoryProducts;
    @Mock private ListKiosqueroOperationalAlerts listKiosqueroOperationalAlerts;
    @Mock private GetKiosqueroDashboard getKiosqueroDashboard;
    @Mock private KiosqueroAccessGuard kiosqueroAccessGuard;
    @Mock private SchoolAccessGuard schoolAccessGuard;
    @Mock private PlanAccessGuard planAccessGuard;

    @InjectMocks
    private KiosqueroOperationsController controller;

    @Test
    void closeDailyShouldDelegateToUseCase() {
        CloseDailyBuffetRequest request = givenCloseDailyRequest();
        CloseDailyBuffetResponse expected = givenCloseDailyResponse(request);

        ResponseEntity<CloseDailyBuffetResponse> response = whenCloseDaily(request);

        thenCloseDailyResponseMatches(response, expected, request);
    }

    @Test
    void dailyCloseStatusShouldDelegateToUseCase() {
        GetDailyBuffetCloseStatusRequest request = givenCloseStatusRequest();
        GetDailyBuffetCloseStatusResponse expected = givenCloseStatusResponse(request);

        ResponseEntity<GetDailyBuffetCloseStatusResponse> response = whenGetCloseStatus(request);

        thenCloseStatusResponseMatches(response, expected, request);
    }

    @Test
    void dailyClosesShouldDelegateToUseCase() {
        ListDailyBuffetClosesRequest request = givenListClosesRequest();
        ListDailyBuffetClosesResponse expected = givenListClosesResponse(request);

        ResponseEntity<List<DailyBuffetCloseResponse>> response = whenListCloses(request);

        thenListClosesResponseMatches(response, expected, request);
    }

    @Test
    void dailyClosesWhenFromIsAfterToShouldThrowException() {
        thenThrowsExceptionForInvalidDates(TO_DATE, FROM_DATE);
    }

    @Test
    void dailyReportShouldDelegateToUseCase() {
        GenerateDailyBuffetReportRequest request = givenReportRequest();
        GenerateDailyBuffetReportResponse expected = givenReportResponse(request);

        ResponseEntity<GenerateDailyBuffetReportResponse> response = whenGetReport(request);

        thenReportResponseMatches(response, expected, request);
    }

    @Test
    void dailyReportCsvShouldReturnCsvWithDownloadHeadersAndDefaultDate() {
        givenCsvReportResponse();

        ResponseEntity<String> response = whenGetCsvReport();

        thenCsvHeadersAndContentAreCorrect(response);
    }

    @Test
    void dashboardShouldDelegateToUseCase() {
        GetKiosqueroDashboardRequest request = givenDashboardRequestWithDate();
        GetKiosqueroDashboardResponse expected = givenDashboardResponse(request);

        ResponseEntity<?> response = whenGetDashboard(request);

        thenDashboardResponseMatchesAndGuarded(response, expected, request);
    }

    @Test
    void dashboardShouldDelegatePeriodToUseCase() {
        GetKiosqueroDashboardRequest request = givenDashboardRequestWithPeriod();
        GetKiosqueroDashboardResponse expected = givenDashboardResponse(request);

        ResponseEntity<?> response = whenGetDashboard(request);

        thenDashboardResponseMatchesAndGuarded(response, expected, request);
    }

    @Test
    void dashboardWhenHomeViewShouldNotRequirePlanAndReturnHomeResponse() {
        GetKiosqueroDashboardRequest request = givenDashboardRequestWithDate();
        GetKiosqueroDashboardResponse expected = givenDashboardResponse(request);

        ResponseEntity<?> response = whenGetHomeDashboard(request);

        thenHomeDashboardResponseMatchesAndIsNotPlanGuarded(response, expected, request);
    }

    @Test
    void alertsShouldReturnMappedOperationalAlerts() {
        givenAlertsResponse();

        ResponseEntity<List<KiosqueroOperationalAlertResponse>> response = whenGetAlerts();

        thenAlertsAreMappedCorrectly(response);
    }

    @Test
    void ordersShouldReturnMappedOrders() {
        givenOrdersResponse();

        ResponseEntity<List<KiosqueroOrderResponse>> response = whenGetOrders();

        thenOrdersAreMappedCorrectly(response);
    }

    @Test
    void timeSlotsShouldReturnMappedSlots() {
        givenTimeSlotsResponse();

        ResponseEntity<List<TimeSlotResponse>> response = whenGetTimeSlots();

        thenTimeSlotsAreMappedCorrectly(response);
    }

    @Test
    void updatePickupSlotRulesShouldDelegateToUseCase() {
        UpdatePickupSlotRulesRequest useCaseRequest = givenUpdateSlotRulesRequest();
        givenUpdateSlotRulesResponse(useCaseRequest);

        ResponseEntity<KiosqueroOperationsController.PickupSlotRulesResponse> response =
                whenUpdateSlotRules();

        thenSlotRulesAreUpdatedAndGuarded(response, useCaseRequest);
    }


    private CloseDailyBuffetRequest givenCloseDailyRequest() {
        return new CloseDailyBuffetRequest(BUFFET_ID, DEFAULT_DATE);
    }

    private CloseDailyBuffetResponse givenCloseDailyResponse(CloseDailyBuffetRequest request) {
        CloseDailyBuffetResponse response = new CloseDailyBuffetResponse(
                false, 1, 0, BigDecimal.ZERO, buildReport(request.buffetId(), request.date())
        );
        when(closeDailyBuffet.execute(request)).thenReturn(response);
        return response;
    }

    private GetDailyBuffetCloseStatusRequest givenCloseStatusRequest() {
        return new GetDailyBuffetCloseStatusRequest(BUFFET_ID, DEFAULT_DATE);
    }

    private GetDailyBuffetCloseStatusResponse givenCloseStatusResponse(
            GetDailyBuffetCloseStatusRequest request) {
        GetDailyBuffetCloseStatusResponse response = new GetDailyBuffetCloseStatusResponse(
                request.buffetId(), request.date(), true, 2, 4, BigDecimal.ZERO
        );
        when(getDailyBuffetCloseStatus.execute(request)).thenReturn(response);
        return response;
    }

    private ListDailyBuffetClosesRequest givenListClosesRequest() {
        return new ListDailyBuffetClosesRequest(BUFFET_ID, FROM_DATE, TO_DATE);
    }

    private ListDailyBuffetClosesResponse givenListClosesResponse(
            ListDailyBuffetClosesRequest request) {
        DailyBuffetClose close = new DailyBuffetClose(
                CLOSE_ID, request.buffetId(), DEFAULT_DATE, 2, 4, BigDecimal.ZERO
        );
        ListDailyBuffetClosesResponse result = 
                ListDailyBuffetClosesResponse.fromDomain(List.of(close));
        when(listDailyBuffetCloses.execute(request)).thenReturn(result);
        return result;
    }

    private GenerateDailyBuffetReportRequest givenReportRequest() {
        return new GenerateDailyBuffetReportRequest(BUFFET_ID, DEFAULT_DATE);
    }

    private GenerateDailyBuffetReportResponse givenReportResponse(
            GenerateDailyBuffetReportRequest request) {
        GenerateDailyBuffetReportResponse report = 
                buildReport(request.buffetId(), request.date());
        when(generateDailyBuffetReport.execute(request)).thenReturn(report);
        return report;
    }

    private void givenCsvReportResponse() {
        GenerateDailyBuffetReportResponse report = new GenerateDailyBuffetReportResponse(
                BUFFET_ID, DEFAULT_DATE, 7, 5, 1, 1, 0, 0, 0, 0,
                new BigDecimal("250.00"), BigDecimal.ZERO, 0,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(generateDailyBuffetReport.execute(any(GenerateDailyBuffetReportRequest.class)))
                .thenReturn(report);
    }

    private GetKiosqueroDashboardRequest givenDashboardRequestWithDate() {
        return new GetKiosqueroDashboardRequest(BUFFET_ID, DEFAULT_DATE, null, null);
    }

    private GetKiosqueroDashboardRequest givenDashboardRequestWithPeriod() {
        return new GetKiosqueroDashboardRequest(BUFFET_ID, null, FROM_DATE, TO_DATE);
    }

    private GetKiosqueroDashboardResponse givenDashboardResponse(
            GetKiosqueroDashboardRequest request) {
        GetKiosqueroDashboardResponse dashboard = buildDashboard(
                request.buffetId(),
                request.date() != null ? request.date() : request.to()
        );
        when(getKiosqueroDashboard.execute(request)).thenReturn(dashboard);
        return dashboard;
    }

    private void givenAlertsResponse() {
        OperationalAlertResponse alert = new OperationalAlertResponse(
                BUFFET_ID,
                OperationalAlertType.DAILY_CAPACITY_LOW.name(),
                OperationalAlertSeverity.WARNING.name(),
                PRODUCT_ID, null, ALERT_TIME, "Cupo bajo"
        );
        ListKiosqueroOperationalAlertsRequest request = 
                new ListKiosqueroOperationalAlertsRequest(BUFFET_ID);
        when(listKiosqueroOperationalAlerts.execute(request))
                .thenReturn(new ListKiosqueroOperationalAlertsResponse(List.of(alert)));
    }

    private void givenOrdersResponse() {
        Purchase purchase = buildPurchase();
        ListKiosqueroOrdersRequest request = buildOrdersRequest();
        when(listKiosqueroOrders.execute(request))
                .thenReturn(ListKiosqueroOrdersResponse.from(List.of(purchase)));
        when(listInventoryProducts.execute(new ListInventoryProductsRequest(BUFFET_ID)))
                .thenReturn(new ListInventoryProductsResponse(List.of()));
    }

    private void givenTimeSlotsResponse() {
        GetTimeSlotsBySchoolResponse slotResponse = new GetTimeSlotsBySchoolResponse(
                SLOT_ID, COLEGIO_ID, "Primer recreo", LocalTime.of(9, 30), LocalTime.of(9, 45),
                true, 30, 10
        );
        when(getBuffetTimeSlots.execute(new GetBuffetTimeSlotsRequest(BUFFET_ID)))
                .thenReturn(List.of(slotResponse));
    }

    private UpdatePickupSlotRulesRequest givenUpdateSlotRulesRequest() {
        return new UpdatePickupSlotRulesRequest(SLOT_ID, 10, 20);
    }

    private void givenUpdateSlotRulesResponse(UpdatePickupSlotRulesRequest request) {
        UpdatePickupSlotRulesResponse response = new UpdatePickupSlotRulesResponse(
                SLOT_ID, null, "Primer recreo", null, null, 10, 20
        );
        when(updatePickupSlotRules.execute(request)).thenReturn(response);
    }

    private GenerateDailyBuffetReportResponse buildReport(UUID buffetId, LocalDate date) {
        return new GenerateDailyBuffetReportResponse(
                buffetId, date, 0, 0, 0, 0, 0, 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, 0,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private GetKiosqueroDashboardResponse buildDashboard(UUID buffetId, LocalDate date) {
        GetKiosqueroDashboardResponse.DashboardProductMetric productMetric =
                new GetKiosqueroDashboardResponse.DashboardProductMetric(
                        PRODUCT_ID, "Alfajor", 3, new BigDecimal("150.00"), "alfajor.png"
                );
        GetKiosqueroDashboardResponse.DashboardInventoryAttention inventoryAttention =
                new GetKiosqueroDashboardResponse.DashboardInventoryAttention(
                        PRODUCT_ID, "Alfajor", 1, 5, "BAJO", "alfajor.png"
                );

        return new GetKiosqueroDashboardResponse(
                buffetId, date,
                new GetKiosqueroDashboardResponse.DashboardSummary(
                        new BigDecimal("250.00"), 7, 4, new BigDecimal("62.50"), 2, 1
                ),
                new GetKiosqueroDashboardResponse.DashboardActivity(
                        List.of(), List.of(), List.of(), List.of()
                ),
                new GetKiosqueroDashboardResponse.DashboardProducts(
                        List.of(productMetric),
                        List.of(productMetric),
                        List.of(inventoryAttention),
                        List.of(inventoryAttention)
                ),
                new GetKiosqueroDashboardResponse.DashboardAlerts(
                        3, 4, new BigDecimal("75.00"), 5, 2, 1, List.of()
                ),
                new GetKiosqueroDashboardResponse.DashboardTrends(List.of())
        );
    }

    private ListKiosqueroOrdersRequest buildOrdersRequest() {
        return new ListKiosqueroOrdersRequest(
                BUFFET_ID, List.of(EstadoCompra.PENDIENTE), DEFAULT_DATE, SLOT_ID, 20
        );
    }

    private Purchase buildPurchase() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("25.00"));

        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setBuffetId(BUFFET_ID);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setTotalAmount(new BigDecimal("25.00"));
        purchase.setDate(ALERT_TIME);
        purchase.setBuyerId(BUYER_ID);
        purchase.setBuyerType(BuyerType.TUTOR);
        purchase.setPickupSlotId(SLOT_ID);
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupSlotStart(LocalTime.of(9, 30));
        purchase.setPickupSlotEnd(LocalTime.of(9, 45));
        purchase.setPickupDate(DEFAULT_DATE);
        purchase.setWithdrawalCode("1234");
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }


    private ResponseEntity<CloseDailyBuffetResponse> whenCloseDaily(
            CloseDailyBuffetRequest request) {
        return controller.closeDaily(request.buffetId(), request.date(), null);
    }

    private ResponseEntity<GetDailyBuffetCloseStatusResponse> whenGetCloseStatus(
            GetDailyBuffetCloseStatusRequest request) {
        return controller.dailyCloseStatus(request.buffetId(), request.date(), null);
    }

    private ResponseEntity<List<DailyBuffetCloseResponse>> whenListCloses(
            ListDailyBuffetClosesRequest request) {
        return controller.dailyCloses(request.buffetId(), request.startDate(), request.endDate(), null);
    }

    private ResponseEntity<GenerateDailyBuffetReportResponse> whenGetReport(
            GenerateDailyBuffetReportRequest request) {
        return controller.dailyReport(request.buffetId(), request.date(), null);
    }

    private ResponseEntity<String> whenGetCsvReport() {
        return controller.dailyReportCsv(BUFFET_ID, null, null);
    }

    private ResponseEntity<?> whenGetDashboard(
            GetKiosqueroDashboardRequest request) {
        return controller.dashboard(
                request.buffetId(), request.date(), request.from(), request.to(), null, null
        );
    }

    private ResponseEntity<?> whenGetHomeDashboard(
            GetKiosqueroDashboardRequest request) {
        return controller.dashboard(
                request.buffetId(), request.date(), request.from(), request.to(), "home", null
        );
    }

    private ResponseEntity<List<KiosqueroOperationalAlertResponse>> whenGetAlerts() {
        return controller.alerts(BUFFET_ID, null);
    }

    private ResponseEntity<List<KiosqueroOrderResponse>> whenGetOrders() {
        return controller.orders(
                BUFFET_ID, List.of(EstadoCompra.PENDIENTE), DEFAULT_DATE, SLOT_ID, 20, null
        );
    }

    private ResponseEntity<List<TimeSlotResponse>> whenGetTimeSlots() {
        return controller.timeSlots(BUFFET_ID, null);
    }

    private ResponseEntity<KiosqueroOperationsController.PickupSlotRulesResponse> whenUpdateSlotRules() {
        return controller.updatePickupSlotRules(
                SLOT_ID, new KiosqueroOperationsController.PickupSlotRulesRequest(10, 20), null
        );
    }


    private void thenCloseDailyResponseMatches(
            ResponseEntity<CloseDailyBuffetResponse> response,
            CloseDailyBuffetResponse expected,
            CloseDailyBuffetRequest request) {
        assertEquals(expected, response.getBody());
        verify(closeDailyBuffet).execute(request);
    }

    private void thenCloseStatusResponseMatches(
            ResponseEntity<GetDailyBuffetCloseStatusResponse> response,
            GetDailyBuffetCloseStatusResponse expected,
            GetDailyBuffetCloseStatusRequest request) {
        assertEquals(expected, response.getBody());
        verify(getDailyBuffetCloseStatus).execute(request);
    }

    private void thenListClosesResponseMatches(
            ResponseEntity<List<DailyBuffetCloseResponse>> response,
            ListDailyBuffetClosesResponse expected,
            ListDailyBuffetClosesRequest request) {
        assertEquals(expected.closes(), response.getBody());
        verify(listDailyBuffetCloses).execute(request);
    }

    private void thenThrowsExceptionForInvalidDates(LocalDate from, LocalDate to) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.dailyCloses(BUFFET_ID, from, to, null)
        );
        assertEquals("startDate must be before or equal to endDate", exception.getMessage());
    }

    private void thenReportResponseMatches(
            ResponseEntity<GenerateDailyBuffetReportResponse> response,
            GenerateDailyBuffetReportResponse expected,
            GenerateDailyBuffetReportRequest request) {
        assertEquals(expected, response.getBody());
        verify(generateDailyBuffetReport).execute(request);
    }

    private void thenCsvHeadersAndContentAreCorrect(ResponseEntity<String> response) {
        assertEquals(MediaType.valueOf("text/csv"), response.getHeaders().getContentType());
        assertEquals(
                "attachment; filename=\"daily-report.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertTrue(response.getBody().contains("metric,value"));
        assertTrue(response.getBody().contains("totalOrders,7"));
        assertTrue(response.getBody().contains("deliveredTotal,250.00"));
        verify(generateDailyBuffetReport).execute(any(GenerateDailyBuffetReportRequest.class));
    }

    private void thenDashboardResponseMatchesAndGuarded(
            ResponseEntity<?> response,
            GetKiosqueroDashboardResponse expected,
            GetKiosqueroDashboardRequest request) {
        assertEquals(expected, response.getBody());
        verify(planAccessGuard).requireVendorIntermediate(null);
        verify(getKiosqueroDashboard).execute(request);
    }

    private void thenHomeDashboardResponseMatchesAndIsNotPlanGuarded(
            ResponseEntity<?> response,
            GetKiosqueroDashboardResponse expected,
            GetKiosqueroDashboardRequest request) {
        assertTrue(response.getBody()
                instanceof KiosqueroOperationsController.KiosqueroDashboardHomeResponse);
        KiosqueroOperationsController.KiosqueroDashboardHomeResponse body =
                (KiosqueroOperationsController.KiosqueroDashboardHomeResponse) response.getBody();

        assertEquals(expected.buffetId(), body.buffetId());
        assertEquals(expected.date(), body.date());
        assertEquals(expected.summary().totalSold(), body.summary().totalSold());
        assertEquals(expected.summary().totalOrders(), body.summary().totalOrders());
        assertEquals(expected.summary().deliveredOrders(), body.summary().deliveredOrders());
        assertEquals(expected.summary().pendingOrders(), body.summary().pendingOrders());
        assertEquals(expected.summary().soldOutProducts(), body.summary().soldOutProducts());
        assertEquals(expected.alerts().pendingOrders(), body.alerts().pendingOrders());
        assertEquals(expected.alerts().readyOrders(), body.alerts().readyOrders());
        assertEquals(expected.alerts().expiredOrders(), body.alerts().expiredOrders());
        assertEquals(
                expected.products().productsNeedingRestock(),
                body.products().productsNeedingRestock()
        );
        assertEquals(expected.products().soldOutProducts(), body.products().soldOutProducts());
        verify(planAccessGuard, never()).requireVendorIntermediate(null);
        verify(getKiosqueroDashboard).execute(request);
    }

    private void thenAlertsAreMappedCorrectly(
            ResponseEntity<List<KiosqueroOperationalAlertResponse>> response) {
        KiosqueroOperationalAlertResponse body = response.getBody().get(0);
        assertEquals(BUFFET_ID, body.buffetId());
        assertEquals("DAILY_CAPACITY_LOW", body.type());
        assertEquals("WARNING", body.severity());
        assertEquals(PRODUCT_ID, body.productId());
        assertEquals("Cupo bajo", body.message());
        
        verify(listKiosqueroOperationalAlerts).execute(
                new ListKiosqueroOperationalAlertsRequest(BUFFET_ID)
        );
    }

    private void thenOrdersAreMappedCorrectly(
            ResponseEntity<List<KiosqueroOrderResponse>> response) {
        KiosqueroOrderResponse order = response.getBody().get(0);
        assertEquals(PURCHASE_ID, order.id());
        assertEquals(BUFFET_ID, order.buffetId());
        assertEquals(EstadoCompra.PENDIENTE, order.status());
        assertEquals(TipoCompra.ANTICIPADA, order.tipo());
        assertEquals(SLOT_ID, order.pickupSlot().id());
        assertEquals("Primer recreo", order.pickupSlot().description());
        assertEquals(1, order.items().size());
        
        verify(listKiosqueroOrders).execute(buildOrdersRequest());
    }

    private void thenTimeSlotsAreMappedCorrectly(
            ResponseEntity<List<TimeSlotResponse>> response) {
        TimeSlotResponse body = response.getBody().get(0);
        assertEquals(SLOT_ID, body.id());
        assertEquals(COLEGIO_ID, body.colegioId());
        assertEquals("Primer recreo", body.descripcion());
        assertEquals(30, body.cupoMaximo());
        assertEquals(10, body.minutosCorte());
        
        verify(getBuffetTimeSlots).execute(new GetBuffetTimeSlotsRequest(BUFFET_ID));
    }

    private void thenSlotRulesAreUpdatedAndGuarded(
            ResponseEntity<KiosqueroOperationsController.PickupSlotRulesResponse> response,
            UpdatePickupSlotRulesRequest request) {
        assertEquals(10, response.getBody().cupoMaximo());
        assertEquals(20, response.getBody().minutosCorte());
        verify(schoolAccessGuard).requireTimeSlotAccess(null, SLOT_ID);
        verify(updatePickupSlotRules).execute(request);
    }
}
