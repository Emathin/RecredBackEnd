package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.CreateTimeSlotRequest;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.DeactivateTimeSlotRequest;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.GetTimeSlotRequest;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.ListSchoolTimeSlotsRequest;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.ReactivateTimeSlotRequest;
import com.recred.backend.application.TimeSlotDetailsResponse;
import com.recred.backend.application.UpdateTimeSlot;
import com.recred.backend.application.UpdateTimeSlotRequest;
import com.recred.backend.infrastructure.web.FranjaHorariaController;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
import com.recred.backend.infrastructure.web.TimeSlotRequest;
import com.recred.backend.infrastructure.web.TimeSlotResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FranjaHorariaControllerTest {

    private final GetTimeSlotsBySchool getTimeSlotsBySchool = mock(GetTimeSlotsBySchool.class);
    private final ListSchoolTimeSlots listSchoolTimeSlots = mock(ListSchoolTimeSlots.class);
    private final GetTimeSlot getTimeSlot = mock(GetTimeSlot.class);
    private final CreateTimeSlot createTimeSlot = mock(CreateTimeSlot.class);
    private final UpdateTimeSlot updateTimeSlot = mock(UpdateTimeSlot.class);
    private final DeactivateTimeSlot deactivateTimeSlot = mock(DeactivateTimeSlot.class);
    private final ReactivateTimeSlot reactivateTimeSlot = mock(ReactivateTimeSlot.class);
    private final SchoolAccessGuard schoolAccessGuard = mock(SchoolAccessGuard.class);
    private final FranjaHorariaController controller = new FranjaHorariaController(
            getTimeSlotsBySchool,
            listSchoolTimeSlots,
            getTimeSlot,
            createTimeSlot,
            updateTimeSlot,
            deactivateTimeSlot,
            reactivateTimeSlot,
            schoolAccessGuard
    );

    @Test
    void obtenerFranjas_WhenIncludeInactive_ShouldRequireAccessAndUseAdminList() {
        UUID colegioId = UUID.randomUUID();
        TimeSlotDetailsResponse slot = response(colegioId, false);
        when(listSchoolTimeSlots.execute(new ListSchoolTimeSlotsRequest(colegioId, true)))
                .thenReturn(List.of(slot));

        ResponseEntity<List<TimeSlotResponse>> result =
                controller.obtenerFranjas(colegioId, true, null);

        assertEquals(1, result.getBody().size());
        assertEquals(false, result.getBody().get(0).activo());
        verify(schoolAccessGuard).requireSchoolAccess(null, colegioId);
        verify(listSchoolTimeSlots).execute(new ListSchoolTimeSlotsRequest(colegioId, true));
    }

    @Test
    void obtenerFranja_ShouldRequireAccessAndReturnSlot() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        GetTimeSlotRequest useCaseRequest = new GetTimeSlotRequest(colegioId, slotId);
        when(getTimeSlot.execute(useCaseRequest)).thenReturn(response(colegioId, slotId, true));

        ResponseEntity<TimeSlotResponse> result =
                controller.obtenerFranja(colegioId, slotId, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(slotId, result.getBody().id());
        verify(schoolAccessGuard).requireSchoolAccess(null, colegioId);
        verify(getTimeSlot).execute(useCaseRequest);
    }

    @Test
    void crearFranja_ShouldRequireAccessAndReturnCreated() {
        UUID colegioId = UUID.randomUUID();
        TimeSlotRequest request = new TimeSlotRequest(
                "Primer recreo",
                LocalTime.of(9, 0),
                LocalTime.of(9, 15),
                30,
                5
        );
        CreateTimeSlotRequest useCaseRequest = new CreateTimeSlotRequest(
                colegioId,
                request.descripcion(),
                request.horaInicio(),
                request.horaFin(),
                request.cupoMaximo(),
                request.minutosCorte()
        );
        when(createTimeSlot.execute(useCaseRequest)).thenReturn(response(colegioId, true));

        ResponseEntity<TimeSlotResponse> result =
                controller.crearFranja(colegioId, request, null);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("Primer recreo", result.getBody().descripcion());
        verify(schoolAccessGuard).requireSchoolAccess(null, colegioId);
        verify(createTimeSlot).execute(useCaseRequest);
    }

    @Test
    void actualizarFranja_ShouldRequireAccessAndReturnUpdatedSlot() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        TimeSlotRequest request = new TimeSlotRequest(
                "Segundo recreo",
                LocalTime.of(10, 0),
                LocalTime.of(10, 15),
                20,
                7
        );
        UpdateTimeSlotRequest useCaseRequest = new UpdateTimeSlotRequest(
                colegioId,
                slotId,
                request.descripcion(),
                request.horaInicio(),
                request.horaFin(),
                request.cupoMaximo(),
                request.minutosCorte()
        );
        when(updateTimeSlot.execute(useCaseRequest)).thenReturn(response(colegioId, slotId, true));

        ResponseEntity<TimeSlotResponse> result =
                controller.actualizarFranja(colegioId, slotId, request, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(slotId, result.getBody().id());
        verify(schoolAccessGuard).requireSchoolAccess(null, colegioId);
        verify(updateTimeSlot).execute(useCaseRequest);
    }

    @Test
    void eliminarFranja_ShouldRequireAccessAndReturnNoContent() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        DeactivateTimeSlotRequest useCaseRequest =
                new DeactivateTimeSlotRequest(colegioId, slotId);

        ResponseEntity<Void> result = controller.eliminarFranja(colegioId, slotId, null);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(schoolAccessGuard).requireSchoolAccess(null, colegioId);
        verify(deactivateTimeSlot).execute(useCaseRequest);
    }

    @Test
    void reactivarFranja_ShouldRequireAccessAndReturnReactivatedSlot() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ReactivateTimeSlotRequest useCaseRequest =
                new ReactivateTimeSlotRequest(colegioId, slotId);
        when(reactivateTimeSlot.execute(useCaseRequest))
                .thenReturn(response(colegioId, slotId, true));

        ResponseEntity<TimeSlotResponse> result =
                controller.reactivarFranja(colegioId, slotId, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody().activo());
        verify(schoolAccessGuard).requireSchoolAccess(null, colegioId);
        verify(reactivateTimeSlot).execute(useCaseRequest);
    }

    private TimeSlotDetailsResponse response(UUID colegioId, boolean active) {
        return response(colegioId, UUID.randomUUID(), active);
    }

    private TimeSlotDetailsResponse response(UUID colegioId, UUID slotId, boolean active) {
        return new TimeSlotDetailsResponse(
                slotId,
                colegioId,
                "Primer recreo",
                LocalTime.of(9, 0),
                LocalTime.of(9, 15),
                active,
                30,
                5
        );
    }
}
