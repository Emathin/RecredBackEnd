package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.CreateTimeSlotRequest;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.ListSchoolTimeSlotsRequest;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.TimeSlotDetailsResponse;
import com.recred.backend.application.UpdateTimeSlot;
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

    private TimeSlotDetailsResponse response(UUID colegioId, boolean active) {
        return new TimeSlotDetailsResponse(
                UUID.randomUUID(),
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
