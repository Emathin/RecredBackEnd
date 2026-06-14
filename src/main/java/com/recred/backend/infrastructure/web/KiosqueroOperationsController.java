package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListKiosqueroOrders;
import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.domain.models.DailyBuffetReport;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyCloseResult;
import com.recred.backend.domain.models.DailyCloseStatus;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.KiosqueroDashboard;
import com.recred.backend.domain.models.Producto;
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
            KiosqueroAccessGuard kiosqueroAccessGuard
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
        Map<UUID, Producto> inventoryProducts = listInventoryProducts.execute(buffetId)
                .stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        List<KiosqueroOrderResponse> orders = listKiosqueroOrders.execute(
                        buffetId,
                        statuses,
                        date,
                        pickupSlotId,
                        limit
                )
                .stream()
                .map(order -> KiosqueroOrderResponse.fromDomain(order, inventoryProducts))
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{buffetId}/alerts")
    public ResponseEntity<List<KiosqueroOperationalAlertResponse>> alerts(
            @PathVariable UUID buffetId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(listKiosqueroOperationalAlerts.execute(buffetId)
                .stream()
                .map(KiosqueroOperationalAlertResponse::fromDomain)
                .toList());
    }

    @GetMapping("/{buffetId}/dashboard")
    public ResponseEntity<KiosqueroDashboard> dashboard(
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
        return ResponseEntity.ok(getKiosqueroDashboard.execute(buffetId, date, from, to));
    }

    @GetMapping("/{buffetId}/time-slots")
    public ResponseEntity<List<TimeSlotResponse>> timeSlots(
            @PathVariable UUID buffetId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(getBuffetTimeSlots.execute(buffetId)
                .stream()
                .map(TimeSlotResponse::fromDomain)
                .toList());
    }

    @PostMapping("/{buffetId}/daily-close")
    public ResponseEntity<DailyCloseResult> closeDaily(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(closeDailyBuffet.execute(buffetId, requestedDate(date)));
    }

    @GetMapping("/{buffetId}/daily-close/status")
    public ResponseEntity<DailyCloseStatus> dailyCloseStatus(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(getDailyBuffetCloseStatus.execute(
                buffetId,
                requestedDate(date)
        ));
    }

    @GetMapping("/{buffetId}/daily-closes")
    public ResponseEntity<List<DailyBuffetClose>> dailyCloses(
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
        return ResponseEntity.ok(listDailyBuffetCloses.execute(buffetId, from, to));
    }

    @GetMapping("/{buffetId}/reports/daily")
    public ResponseEntity<DailyBuffetReport> dailyReport(
            @PathVariable UUID buffetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(generateDailyBuffetReport.execute(buffetId,
                requestedDate(date)));
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
        DailyBuffetReport report = generateDailyBuffetReport.execute(
                buffetId,
                requestedDate(date)
        );
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"daily-report.csv\"")
                .body(csv(report));
    }

    @PatchMapping("/time-slots/{slotId}/rules")
    public ResponseEntity<PickupSlotRulesResponse> updatePickupSlotRules(
            @PathVariable UUID slotId,
            @Valid @RequestBody PickupSlotRulesRequest request
    ) {
        FranjaHoraria slot = updatePickupSlotRules.execute(
                slotId,
                request.cupoMaximo(),
                request.minutosCorte()
        );
        return ResponseEntity.ok(PickupSlotRulesResponse.fromDomain(slot));
    }

    private LocalDate requestedDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private String csv(DailyBuffetReport report) {
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
        static PickupSlotRulesResponse fromDomain(FranjaHoraria slot) {
            return new PickupSlotRulesResponse(
                    slot.getId(),
                    slot.getColegioId(),
                    slot.getDescripcion(),
                    slot.getHoraInicio(),
                    slot.getHoraFin(),
                    slot.getCupoMaximo(),
                    slot.getMinutosCorte()
            );
        }
    }
}
