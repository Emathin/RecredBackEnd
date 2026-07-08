package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.CloseDailyBuffetRequest;
import com.recred.backend.application.CloseDailyBuffetResponse;
import com.recred.backend.application.DailyBuffetCloseResponse;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GenerateDailyBuffetReportRequest;
import com.recred.backend.application.GenerateDailyBuffetReportResponse;
import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetDailyBuffetCloseStatusRequest;
import com.recred.backend.application.GetDailyBuffetCloseStatusResponse;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetBuffetTimeSlotsRequest;
import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.application.GetKiosqueroDashboardRequest;
import com.recred.backend.application.GetKiosqueroDashboardResponse;
import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListDailyBuffetClosesRequest;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListInventoryProductsRequest;
import com.recred.backend.application.ListInventoryProductsResponse.InventoryProductResponse;
import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.ListKiosqueroOrdersRequest;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.ListKiosqueroOperationalAlertsRequest;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.application.UpdatePickupSlotRulesRequest;
import com.recred.backend.application.UpdatePickupSlotRulesResponse;
import com.recred.backend.domain.models.EstadoCompra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/kiosqueros")
public class KiosqueroOperationsController {

    private final CloseDailyBuffet closeDailyBuffet;
    private final GetDailyBuffetCloseStatus getDailyBuffetCloseStatus;
    private final ListDailyBuffetCloses listDailyBuffetCloses;
    private final GenerateDailyBuffetReport generateDailyBuffetReport;
    private final UpdatePickupSlotRules updatePickupSlotRules;
    private final ListKiosqueroOrders listKiosqueroOrders;
    private final GetBuffetTimeSlots getBuffetTimeSlots;
    private final ListInventoryProducts listInventoryProducts;
    private final ListKiosqueroOperationalAlerts listKiosqueroOperationalAlerts;
    private final GetKiosqueroDashboard getKiosqueroDashboard;
    private final KiosqueroAccessGuard kiosqueroAccessGuard;
    private final SchoolAccessGuard schoolAccessGuard;
    private final PlanAccessGuard planAccessGuard;

    public KiosqueroOperationsController(
            CloseDailyBuffet closeDailyBuffet,
            GetDailyBuffetCloseStatus getDailyBuffetCloseStatus,
            ListDailyBuffetCloses listDailyBuffetCloses,
            GenerateDailyBuffetReport generateDailyBuffetReport,
            UpdatePickupSlotRules updatePickupSlotRules,
            ListKiosqueroOrders listKiosqueroOrders,
            GetBuffetTimeSlots getBuffetTimeSlots,
            ListInventoryProducts listInventoryProducts,
            ListKiosqueroOperationalAlerts listKiosqueroOperationalAlerts,
            GetKiosqueroDashboard getKiosqueroDashboard,
            KiosqueroAccessGuard kiosqueroAccessGuard,
            SchoolAccessGuard schoolAccessGuard,
            PlanAccessGuard planAccessGuard
    ) {
        this.closeDailyBuffet = closeDailyBuffet;
        this.getDailyBuffetCloseStatus = getDailyBuffetCloseStatus;
        this.listDailyBuffetCloses = listDailyBuffetCloses;
        this.generateDailyBuffetReport = generateDailyBuffetReport;
        this.updatePickupSlotRules = updatePickupSlotRules;
        this.listKiosqueroOrders = listKiosqueroOrders;
        this.getBuffetTimeSlots = getBuffetTimeSlots;
        this.listInventoryProducts = listInventoryProducts;
        this.listKiosqueroOperationalAlerts = listKiosqueroOperationalAlerts;
        this.getKiosqueroDashboard = getKiosqueroDashboard;
        this.kiosqueroAccessGuard = kiosqueroAccessGuard;
        this.schoolAccessGuard = schoolAccessGuard;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping("/{buffetId}/orders")
    public ResponseEntity<List<KiosqueroOrderResponse>> orders(
            @PathVariable UUID buffetId,
            @RequestParam(required = false) List<EstadoCompra> statuses,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) UUID pickupSlotId,
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        Map<UUID, InventoryProductResponse> inventoryProducts = listInventoryProducts
                .execute(new ListInventoryProductsRequest(buffetId))
                .products()
                .stream()
                .collect(Collectors.toMap(InventoryProductResponse::id, Function.identity()));
        ListKiosqueroOrdersRequest request =
                new ListKiosqueroOrdersRequest(buffetId, statuses, date, pickupSlotId, limit);
        List<KiosqueroOrderResponse> orders = listKiosqueroOrders.execute(request)
                .orders()
                .stream()
                .map(order -> KiosqueroOrderResponse.fromApplication(order, inventoryProducts))
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{buffetId}/alerts")
    public ResponseEntity<List<KiosqueroOperationalAlertResponse>> alerts(
            @PathVariable UUID buffetId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(listKiosqueroOperationalAlerts
                .execute(new ListKiosqueroOperationalAlertsRequest(buffetId))
                .alerts()
                .stream()
                .map(KiosqueroOperationalAlertResponse::fromApplication)
                .toList());
    }

    @GetMapping("/{buffetId}/dashboard")
    public ResponseEntity<GetKiosqueroDashboardResponse> dashboard(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        planAccessGuard.requireVendorIntermediate(authentication);
        GetKiosqueroDashboardRequest request =
                new GetKiosqueroDashboardRequest(buffetId, date, from, to);
        return ResponseEntity.ok(getKiosqueroDashboard.execute(request));
    }

    @GetMapping("/{buffetId}/time-slots")
    public ResponseEntity<List<TimeSlotResponse>> timeSlots(
            @PathVariable UUID buffetId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(getBuffetTimeSlots
                .execute(new GetBuffetTimeSlotsRequest(buffetId))
                .stream()
                .map(TimeSlotResponse::fromUseCaseResponse)
                .toList());
    }

    @PostMapping("/{buffetId}/daily-close")
    public ResponseEntity<CloseDailyBuffetResponse> closeDaily(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(closeDailyBuffet.execute(
                new CloseDailyBuffetRequest(buffetId, requestedDate(date))));
    }

    @GetMapping("/{buffetId}/daily-close/status")
    public ResponseEntity<GetDailyBuffetCloseStatusResponse> dailyCloseStatus(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(getDailyBuffetCloseStatus.execute(
                new GetDailyBuffetCloseStatusRequest(buffetId, requestedDate(date))));
    }

    @GetMapping("/{buffetId}/daily-closes")
    public ResponseEntity<List<DailyBuffetCloseResponse>> dailyCloses(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(listDailyBuffetCloses.execute(
                new ListDailyBuffetClosesRequest(buffetId, from, to)).closes());
    }

    @GetMapping("/{buffetId}/reports/daily")
    public ResponseEntity<GenerateDailyBuffetReportResponse> dailyReport(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(generateDailyBuffetReport.execute(
                new GenerateDailyBuffetReportRequest(buffetId, requestedDate(date))));
    }

    @GetMapping("/{buffetId}/reports/daily.csv")
    public ResponseEntity<String> dailyReportCsv(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        GenerateDailyBuffetReportResponse report = generateDailyBuffetReport.execute(
                new GenerateDailyBuffetReportRequest(buffetId, requestedDate(date)));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"daily-report.csv\"")
                .body(csv(report));
    }

    @PatchMapping("/time-slots/{slotId}/rules")
    public ResponseEntity<PickupSlotRulesResponse> updatePickupSlotRules(
            @PathVariable UUID slotId,
            @Valid @RequestBody PickupSlotRulesRequest request,
            Authentication authentication
    ) {
        schoolAccessGuard.requireTimeSlotAccess(authentication, slotId);
        UpdatePickupSlotRulesResponse slot = updatePickupSlotRules.execute(
                new UpdatePickupSlotRulesRequest(
                        slotId,
                        request.cupoMaximo(),
                        request.minutosCorte()
                ));
        return ResponseEntity.ok(PickupSlotRulesResponse.fromApplication(slot));
    }

    private LocalDate requestedDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private String csv(GenerateDailyBuffetReportResponse report) {
        return String.join("\n",
                "metric,value",
                "buffetId," + report.buffetId(),
                "date," + report.date(),
                "totalOrders," + report.totalOrders(),
                "deliveredOrders," + report.deliveredOrders(),
                "expiredOrders," + report.expiredOrders(),
                "cancelledOrders," + report.cancelledOrders(),
                "rejectedOrders," + report.rejectedOrders(),
                "deliveredTotal," + report.deliveredTotal(),
                "refundedCredits," + report.refundedCredits(),
                "releasedReservations," + report.releasedReservations()
        );
    }

    public record PickupSlotRulesRequest(
            @PositiveOrZero Integer cupoMaximo,
            @PositiveOrZero Integer minutosCorte
    ) {
    }

    public record PickupSlotRulesResponse(
            UUID id,
            UUID colegioId,
            String descripcion,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin,
            Integer cupoMaximo,
            Integer minutosCorte
    ) {
        static PickupSlotRulesResponse fromApplication(UpdatePickupSlotRulesResponse slot) {
            return new PickupSlotRulesResponse(
                    slot.id(),
                    slot.colegioId(),
                    slot.descripcion(),
                    slot.horaInicio(),
                    slot.horaFin(),
                    slot.cupoMaximo(),
                    slot.minutosCorte()
            );
        }
    }
}
