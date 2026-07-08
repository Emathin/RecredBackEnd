package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.CreateTimeSlotRequest;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.DeactivateTimeSlotRequest;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.GetTimeSlotRequest;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTimeSlotsBySchoolRequest;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.ListSchoolTimeSlotsRequest;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.ReactivateTimeSlotRequest;
import com.recred.backend.application.TimeSlotDetailsResponse;
import com.recred.backend.application.UpdateTimeSlot;
import com.recred.backend.application.UpdateTimeSlotRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({
        "/api/colegios/{colegioId}/franjas-horarias",
        "/api/v1/colegios/{colegioId}/franjas-horarias"
})
public class FranjaHorariaController {

    private final GetTimeSlotsBySchool getTimeSlotsBySchool;
    private final ListSchoolTimeSlots listSchoolTimeSlots;
    private final GetTimeSlot getTimeSlot;
    private final CreateTimeSlot createTimeSlot;
    private final UpdateTimeSlot updateTimeSlot;
    private final DeactivateTimeSlot deactivateTimeSlot;
    private final ReactivateTimeSlot reactivateTimeSlot;
    private final SchoolAccessGuard schoolAccessGuard;

    public FranjaHorariaController(
            GetTimeSlotsBySchool getTimeSlotsBySchool,
            ListSchoolTimeSlots listSchoolTimeSlots,
            GetTimeSlot getTimeSlot,
            CreateTimeSlot createTimeSlot,
            UpdateTimeSlot updateTimeSlot,
            DeactivateTimeSlot deactivateTimeSlot,
            ReactivateTimeSlot reactivateTimeSlot,
            SchoolAccessGuard schoolAccessGuard
    ) {
        this.getTimeSlotsBySchool = getTimeSlotsBySchool;
        this.listSchoolTimeSlots = listSchoolTimeSlots;
        this.getTimeSlot = getTimeSlot;
        this.createTimeSlot = createTimeSlot;
        this.updateTimeSlot = updateTimeSlot;
        this.deactivateTimeSlot = deactivateTimeSlot;
        this.reactivateTimeSlot = reactivateTimeSlot;
        this.schoolAccessGuard = schoolAccessGuard;
    }

    @GetMapping
    public ResponseEntity<List<TimeSlotResponse>> obtenerFranjas(
            @PathVariable UUID colegioId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Authentication authentication
    ) {
        if (includeInactive) {
            schoolAccessGuard.requireSchoolAccess(authentication, colegioId);
        }

        List<TimeSlotResponse> franjas = slots(colegioId, includeInactive)
                .stream()
                .map(TimeSlotResponse::fromApplication)
                .toList();
        return ResponseEntity.ok(franjas);
    }

    @GetMapping("/{slotId}")
    public ResponseEntity<TimeSlotResponse> obtenerFranja(
            @PathVariable UUID colegioId,
            @PathVariable UUID slotId,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolAccess(authentication, colegioId);
        return ResponseEntity.ok(TimeSlotResponse.fromApplication(
                getTimeSlot.execute(new GetTimeSlotRequest(colegioId, slotId))));
    }

    @PostMapping
    public ResponseEntity<TimeSlotResponse> crearFranja(
            @PathVariable UUID colegioId,
            @Valid @RequestBody TimeSlotRequest request,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolAccess(authentication, colegioId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TimeSlotResponse.fromApplication(createTimeSlot.execute(
                        new CreateTimeSlotRequest(
                                colegioId,
                                request.descripcion(),
                                request.horaInicio(),
                                request.horaFin(),
                                request.cupoMaximo(),
                                request.minutosCorte()
                        ))));
    }

    @PutMapping("/{slotId}")
    public ResponseEntity<TimeSlotResponse> actualizarFranja(
            @PathVariable UUID colegioId,
            @PathVariable UUID slotId,
            @Valid @RequestBody TimeSlotRequest request,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolAccess(authentication, colegioId);
        return ResponseEntity.ok(TimeSlotResponse.fromApplication(updateTimeSlot.execute(
                new UpdateTimeSlotRequest(
                        colegioId,
                        slotId,
                        request.descripcion(),
                        request.horaInicio(),
                        request.horaFin(),
                        request.cupoMaximo(),
                        request.minutosCorte()
                ))));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> eliminarFranja(
            @PathVariable UUID colegioId,
            @PathVariable UUID slotId,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolAccess(authentication, colegioId);
        deactivateTimeSlot.execute(new DeactivateTimeSlotRequest(colegioId, slotId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{slotId}/reactivar")
    public ResponseEntity<TimeSlotResponse> reactivarFranja(
            @PathVariable UUID colegioId,
            @PathVariable UUID slotId,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolAccess(authentication, colegioId);
        return ResponseEntity.ok(TimeSlotResponse.fromApplication(
                reactivateTimeSlot.execute(new ReactivateTimeSlotRequest(colegioId, slotId))));
    }

    private List<TimeSlotDetailsResponse> slots(
            UUID colegioId,
            boolean includeInactive
    ) {
        if (includeInactive) {
            return listSchoolTimeSlots.execute(new ListSchoolTimeSlotsRequest(
                    colegioId,
                    true));
        }

        return getTimeSlotsBySchool.execute(new GetTimeSlotsBySchoolRequest(colegioId))
                .stream()
                .map(slot -> new TimeSlotDetailsResponse(
                        slot.id(),
                        slot.colegioId(),
                        slot.descripcion(),
                        slot.horaInicio(),
                        slot.horaFin(),
                        slot.activo(),
                        slot.cupoMaximo(),
                        slot.minutosCorte()))
                .toList();
    }
}
