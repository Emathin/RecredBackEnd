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
import org.junit.jupiter.api.BeforeEach;
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

    private GetTimeSlotsBySchool getTimeSlotsBySchool;
    private ListSchoolTimeSlots listSchoolTimeSlots;
    private GetTimeSlot getTimeSlot;
    private CreateTimeSlot createTimeSlot;
    private UpdateTimeSlot updateTimeSlot;
    private DeactivateTimeSlot deactivateTimeSlot;
    private ReactivateTimeSlot reactivateTimeSlot;
    private SchoolAccessGuard schoolAccessGuard;
    private FranjaHorariaController controller;

    private final UUID SCHOOL_ID = UUID.randomUUID();
    private final UUID SLOT_ID = UUID.randomUUID();
    private final String RECREO_1 = "Primer recreo";
    private final String RECREO_2 = "Segundo recreo";
    private final LocalTime T_9_00 = LocalTime.of(9, 0);
    private final LocalTime T_9_15 = LocalTime.of(9, 15);
    private final LocalTime T_10_00 = LocalTime.of(10, 0);
    private final LocalTime T_10_15 = LocalTime.of(10, 15);

    @BeforeEach
    void setUp() {
        getTimeSlotsBySchool = mock(GetTimeSlotsBySchool.class);
        listSchoolTimeSlots = mock(ListSchoolTimeSlots.class);
        getTimeSlot = mock(GetTimeSlot.class);
        createTimeSlot = mock(CreateTimeSlot.class);
        updateTimeSlot = mock(UpdateTimeSlot.class);
        deactivateTimeSlot = mock(DeactivateTimeSlot.class);
        reactivateTimeSlot = mock(ReactivateTimeSlot.class);
        schoolAccessGuard = mock(SchoolAccessGuard.class);
        
        controller = new FranjaHorariaController(
                getTimeSlotsBySchool,
                listSchoolTimeSlots,
                getTimeSlot,
                createTimeSlot,
                updateTimeSlot,
                deactivateTimeSlot,
                reactivateTimeSlot,
                schoolAccessGuard
        );
    }

    @Test
    void obtenerFranjasShouldRequireAccessAndUseAdminListWhenIncludeInactive() {
        givenListSchoolTimeSlotsReturnsInactiveSlot();
        
        ResponseEntity<List<TimeSlotResponse>> result = whenGettingTimeSlotsIncludingInactive();
        
        thenListResponseHasInactiveSlot(result);
        thenDelegatesToListSchoolTimeSlots();
    }

    @Test
    void obtenerFranjaShouldRequireAccessAndReturnSlot() {
        givenGetTimeSlotReturnsSlot();
        
        ResponseEntity<TimeSlotResponse> result = whenGettingTimeSlot();
        
        thenSlotResponseIsOk(result);
        thenDelegatesToGetTimeSlot();
    }

    @Test
    void crearFranjaShouldRequireAccessAndReturnCreated() {
        TimeSlotRequest request = createRequest(RECREO_1, T_9_00, T_9_15, 30, 5);
        givenCreateTimeSlotSucceeds(request);
        
        ResponseEntity<TimeSlotResponse> result = whenCreatingTimeSlot(request);
        
        thenCreateResponseIsCreated(result, RECREO_1);
        thenDelegatesToCreateTimeSlot(request);
    }

    @Test
    void actualizarFranjaShouldRequireAccessAndReturnUpdatedSlot() {
        TimeSlotRequest request = createRequest(RECREO_2, T_10_00, T_10_15, 20, 7);
        givenUpdateTimeSlotSucceeds(request);
        
        ResponseEntity<TimeSlotResponse> result = whenUpdatingTimeSlot(request);
        
        thenUpdateResponseIsOk(result);
        thenDelegatesToUpdateTimeSlot(request);
    }

    @Test
    void eliminarFranjaShouldRequireAccessAndReturnNoContent() {
        ResponseEntity<Void> result = whenDeletingTimeSlot();
        
        thenResponseIsNoContent(result);
        thenDelegatesToDeactivateTimeSlot();
    }

    @Test
    void reactivarFranjaShouldRequireAccessAndReturnReactivatedSlot() {
        givenReactivateTimeSlotSucceeds();
        
        ResponseEntity<TimeSlotResponse> result = whenReactivatingTimeSlot();
        
        thenReactivateResponseIsOk(result);
        thenDelegatesToReactivateTimeSlot();
    }

    private void givenListSchoolTimeSlotsReturnsInactiveSlot() {
        TimeSlotDetailsResponse slot = response(SCHOOL_ID, false);
        when(listSchoolTimeSlots.execute(new ListSchoolTimeSlotsRequest(SCHOOL_ID, true)))
                .thenReturn(List.of(slot));
    }

    private void givenGetTimeSlotReturnsSlot() {
        GetTimeSlotRequest useCaseRequest = new GetTimeSlotRequest(SCHOOL_ID, SLOT_ID);
        when(getTimeSlot.execute(useCaseRequest)).thenReturn(response(SCHOOL_ID, SLOT_ID, true));
    }

    private void givenCreateTimeSlotSucceeds(TimeSlotRequest request) {
        CreateTimeSlotRequest useCaseRequest = new CreateTimeSlotRequest(
                SCHOOL_ID, request.descripcion(), request.horaInicio(),
                request.horaFin(), request.cupoMaximo(), request.minutosCorte()
        );
        when(createTimeSlot.execute(useCaseRequest)).thenReturn(response(SCHOOL_ID, true));
    }

    private void givenUpdateTimeSlotSucceeds(TimeSlotRequest request) {
        UpdateTimeSlotRequest useCaseRequest = new UpdateTimeSlotRequest(
                SCHOOL_ID, SLOT_ID, request.descripcion(), request.horaInicio(),
                request.horaFin(), request.cupoMaximo(), request.minutosCorte()
        );
        when(updateTimeSlot.execute(useCaseRequest)).thenReturn(response(SCHOOL_ID, SLOT_ID, true));
    }

    private void givenReactivateTimeSlotSucceeds() {
        ReactivateTimeSlotRequest useCaseRequest = new ReactivateTimeSlotRequest(SCHOOL_ID, SLOT_ID);
        when(reactivateTimeSlot.execute(useCaseRequest)).thenReturn(response(SCHOOL_ID, SLOT_ID, true));
    }

    private ResponseEntity<List<TimeSlotResponse>> whenGettingTimeSlotsIncludingInactive() {
        return controller.obtenerFranjas(SCHOOL_ID, true, null);
    }

    private ResponseEntity<TimeSlotResponse> whenGettingTimeSlot() {
        return controller.obtenerFranja(SCHOOL_ID, SLOT_ID, null);
    }

    private ResponseEntity<TimeSlotResponse> whenCreatingTimeSlot(TimeSlotRequest request) {
        return controller.crearFranja(SCHOOL_ID, request, null);
    }

    private ResponseEntity<TimeSlotResponse> whenUpdatingTimeSlot(TimeSlotRequest request) {
        return controller.actualizarFranja(SCHOOL_ID, SLOT_ID, request, null);
    }

    private ResponseEntity<Void> whenDeletingTimeSlot() {
        return controller.eliminarFranja(SCHOOL_ID, SLOT_ID, null);
    }

    private ResponseEntity<TimeSlotResponse> whenReactivatingTimeSlot() {
        return controller.reactivarFranja(SCHOOL_ID, SLOT_ID, null);
    }

    private void thenListResponseHasInactiveSlot(ResponseEntity<List<TimeSlotResponse>> result) {
        assertEquals(1, result.getBody().size());
        assertEquals(false, result.getBody().get(0).activo());
    }

    private void thenDelegatesToListSchoolTimeSlots() {
        verify(schoolAccessGuard).requireSchoolAccess(null, SCHOOL_ID);
        verify(listSchoolTimeSlots).execute(new ListSchoolTimeSlotsRequest(SCHOOL_ID, true));
    }

    private void thenSlotResponseIsOk(ResponseEntity<TimeSlotResponse> result) {
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(SLOT_ID, result.getBody().id());
    }

    private void thenDelegatesToGetTimeSlot() {
        verify(schoolAccessGuard).requireSchoolAccess(null, SCHOOL_ID);
        verify(getTimeSlot).execute(new GetTimeSlotRequest(SCHOOL_ID, SLOT_ID));
    }

    private void thenCreateResponseIsCreated(ResponseEntity<TimeSlotResponse> result, String desc) {
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(desc, result.getBody().descripcion());
    }

    private void thenDelegatesToCreateTimeSlot(TimeSlotRequest request) {
        verify(schoolAccessGuard).requireSchoolAccess(null, SCHOOL_ID);
        verify(createTimeSlot).execute(new CreateTimeSlotRequest(
                SCHOOL_ID, request.descripcion(), request.horaInicio(),
                request.horaFin(), request.cupoMaximo(), request.minutosCorte()));
    }

    private void thenUpdateResponseIsOk(ResponseEntity<TimeSlotResponse> result) {
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(SLOT_ID, result.getBody().id());
    }

    private void thenDelegatesToUpdateTimeSlot(TimeSlotRequest request) {
        verify(schoolAccessGuard).requireSchoolAccess(null, SCHOOL_ID);
        verify(updateTimeSlot).execute(new UpdateTimeSlotRequest(
                SCHOOL_ID, SLOT_ID, request.descripcion(), request.horaInicio(),
                request.horaFin(), request.cupoMaximo(), request.minutosCorte()));
    }

    private void thenResponseIsNoContent(ResponseEntity<Void> result) {
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    private void thenDelegatesToDeactivateTimeSlot() {
        verify(schoolAccessGuard).requireSchoolAccess(null, SCHOOL_ID);
        verify(deactivateTimeSlot).execute(new DeactivateTimeSlotRequest(SCHOOL_ID, SLOT_ID));
    }

    private void thenReactivateResponseIsOk(ResponseEntity<TimeSlotResponse> result) {
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody().activo());
    }

    private void thenDelegatesToReactivateTimeSlot() {
        verify(schoolAccessGuard).requireSchoolAccess(null, SCHOOL_ID);
        verify(reactivateTimeSlot).execute(new ReactivateTimeSlotRequest(SCHOOL_ID, SLOT_ID));
    }

    private TimeSlotRequest createRequest(String desc, LocalTime start, LocalTime end, int max, int cutoff) {
        return new TimeSlotRequest(desc, start, end, max, cutoff);
    }

    private TimeSlotDetailsResponse response(UUID colegioId, boolean active) {
        return response(colegioId, UUID.randomUUID(), active);
    }

    private TimeSlotDetailsResponse response(UUID colegioId, UUID slotId, boolean active) {
        return new TimeSlotDetailsResponse(
                slotId, colegioId, RECREO_1, T_9_00, T_9_15, active, 30, 5
        );
    }
}
