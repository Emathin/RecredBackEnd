package com.recred.backend.unit.application;

import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.CreateTimeSlotRequest;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.DeactivateTimeSlotRequest;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.ListSchoolTimeSlotsRequest;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.ReactivateTimeSlotRequest;
import com.recred.backend.application.TimeSlotDetailsResponse;
import com.recred.backend.application.UpdateTimeSlot;
import com.recred.backend.application.UpdateTimeSlotRequest;
import com.recred.backend.domain.exceptions.TimeSlotInUseException;
import com.recred.backend.domain.exceptions.TimeSlotOverlapException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeSlotManagementUseCasesTest {

    private static final UUID COLEGIO_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final String DESCRIPCION = "Primer recreo";
    private static final LocalTime INICIO = LocalTime.of(9, 0);
    private static final LocalTime FIN = LocalTime.of(9, 15);

    private final FranjaHorariaRepository franjaHorariaRepository =
            mock(FranjaHorariaRepository.class);
    private final ColegioRepository colegioRepository = mock(ColegioRepository.class);
    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void create_ShouldSaveActiveTimeSlotForActiveSchool() {
        givenActiveSchool();
        givenNoActiveOverlapForCreation();
        givenSavedSlotReturnsWithId();

        TimeSlotDetailsResponse response = whenCreatingTimeSlot(DESCRIPCION);

        thenResponseHasDetails(response, "Primer recreo", 0, true);
    }

    @Test
    void create_WhenOverlapsActiveSlot_ShouldThrowException() {
        givenActiveSchool();
        givenActiveOverlapForCreation();

        Executable execution = () -> whenCreatingTimeSlot(DESCRIPCION);

        thenThrowsOverlapException(execution);
        thenSlotWasNotSaved();
    }

    @Test
    void update_WhenStructuralDataChangesAndHasFuturePurchases_ShouldThrowException() {
        givenActiveSchool();
        givenExistingSlot(true);
        givenFuturePurchasesExist();

        Executable execution = () -> whenUpdatingTimeSlot("Segundo recreo", LocalTime.of(10, 0), LocalTime.of(10, 15));

        thenThrowsInUseException(execution);
        thenSlotWasNotSaved();
    }

    @Test
    void update_WhenOnlyRulesChange_ShouldSaveWithoutCheckingFuturePurchases() {
        givenActiveSchool();
        givenExistingSlot(true);
        givenNoActiveOverlapForUpdate();
        givenSavedSlotReturnsItself();

        TimeSlotDetailsResponse response = whenUpdatingTimeSlot(DESCRIPCION, INICIO, FIN);

        thenResponseHasRulesUpdated(response, 40, 5);
        thenFuturePurchasesWereNotChecked();
    }

    @Test
    void deactivate_ShouldSaveInactiveSlot() {
        givenExistingSlot(true);
        givenSavedSlotReturnsItself();

        TimeSlotDetailsResponse response = whenDeactivatingTimeSlot();

        thenResponseIsInactive(response);
        thenSlotWasSaved();
    }

    @Test
    void reactivate_WhenOverlapsActiveSlot_ShouldThrowException() {
        givenExistingSlot(false);
        givenActiveOverlapForUpdate();

        Executable execution = () -> whenReactivatingTimeSlot();

        thenThrowsOverlapException(execution);
        thenSlotWasNotSaved();
    }

    @Test
    void list_WhenIncludesInactive_ShouldUseAdministrativeQuery() {
        givenAdministrativeSlotsFound();

        List<TimeSlotDetailsResponse> response = whenListingAdministrativeTimeSlots();

        thenResponseContainsOneInactiveSlot(response);
        thenAdministrativeQueryWasUsed();
    }

    private void givenActiveSchool() {
        when(colegioRepository.existsActiveById(COLEGIO_ID)).thenReturn(true);
    }

    private void givenNoActiveOverlapForCreation() {
        when(franjaHorariaRepository.existsActiveOverlap(
                eq(COLEGIO_ID), eq(null), eq(INICIO), eq(FIN))).thenReturn(false);
    }

    private void givenActiveOverlapForCreation() {
        when(franjaHorariaRepository.existsActiveOverlap(
                eq(COLEGIO_ID), eq(null), eq(INICIO), eq(FIN))).thenReturn(true);
    }

    private void givenNoActiveOverlapForUpdate() {
        when(franjaHorariaRepository.existsActiveOverlap(
                COLEGIO_ID, SLOT_ID, INICIO, FIN)).thenReturn(false);
    }

    private void givenActiveOverlapForUpdate() {
        when(franjaHorariaRepository.existsActiveOverlap(
                COLEGIO_ID, SLOT_ID, INICIO, FIN)).thenReturn(true);
    }

    private void givenExistingSlot(boolean isActivo) {
        when(franjaHorariaRepository.findByIdAndColegioId(SLOT_ID, COLEGIO_ID))
                .thenReturn(Optional.of(slotEntity(isActivo)));
    }

    private void givenFuturePurchasesExist() {
        when(purchaseRepository.existsActiveFutureScheduledByPickupSlotId(
                eq(SLOT_ID), eq(LocalDate.of(2026, 7, 2)), any())).thenReturn(true);
    }

    private void givenSavedSlotReturnsWithId() {
        when(franjaHorariaRepository.save(any(FranjaHoraria.class))).thenAnswer(invocation -> {
            FranjaHoraria slot = invocation.getArgument(0);
            slot.setId(SLOT_ID);
            return slot;
        });
    }

    private void givenSavedSlotReturnsItself() {
        when(franjaHorariaRepository.save(any(FranjaHoraria.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void givenAdministrativeSlotsFound() {
        when(franjaHorariaRepository.findAllByColegioIdIncludingInactive(COLEGIO_ID))
                .thenReturn(List.of(slotEntity(false)));
    }

    private FranjaHoraria slotEntity(boolean activo) {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(SLOT_ID);
        slot.setColegioId(COLEGIO_ID);
        slot.setDescripcion(DESCRIPCION);
        slot.setHoraInicio(INICIO);
        slot.setHoraFin(FIN);
        slot.setActivo(activo);
        return slot;
    }


    private TimeSlotDetailsResponse whenCreatingTimeSlot(String descripcion) {
        CreateTimeSlot useCase = new CreateTimeSlot(franjaHorariaRepository, colegioRepository);
        return useCase.execute(new CreateTimeSlotRequest(
                COLEGIO_ID, descripcion, INICIO, FIN, 30, null));
    }

    private TimeSlotDetailsResponse whenUpdatingTimeSlot(String descripcion, LocalTime start, LocalTime end) {
        UpdateTimeSlot useCase = new UpdateTimeSlot(
                franjaHorariaRepository, colegioRepository, purchaseRepository, clock);
        return useCase.execute(new UpdateTimeSlotRequest(
                COLEGIO_ID, SLOT_ID, descripcion, start, end, 40, 5));
    }

    private TimeSlotDetailsResponse whenDeactivatingTimeSlot() {
        DeactivateTimeSlot useCase = new DeactivateTimeSlot(franjaHorariaRepository);
        return useCase.execute(new DeactivateTimeSlotRequest(COLEGIO_ID, SLOT_ID));
    }

    private TimeSlotDetailsResponse whenReactivatingTimeSlot() {
        ReactivateTimeSlot useCase = new ReactivateTimeSlot(franjaHorariaRepository);
        return useCase.execute(new ReactivateTimeSlotRequest(COLEGIO_ID, SLOT_ID));
    }

    private List<TimeSlotDetailsResponse> whenListingAdministrativeTimeSlots() {
        ListSchoolTimeSlots useCase = new ListSchoolTimeSlots(franjaHorariaRepository);
        return useCase.execute(new ListSchoolTimeSlotsRequest(COLEGIO_ID, true));
    }

    // --- Helpers Privados: Then ---

    private void thenResponseHasDetails(
            TimeSlotDetailsResponse response,
            String expectedDesc,
            int expectedCutoff,
            boolean expectedActive) {
        assertEquals(SLOT_ID, response.id());
        assertEquals(expectedDesc, response.descripcion());
        assertEquals(expectedCutoff, response.minutosCorte());
        assertEquals(expectedActive, response.activo());
    }

    private void thenResponseHasRulesUpdated(
            TimeSlotDetailsResponse response,
            int expectedMax,
            int expectedCutoff) {
        assertEquals(expectedMax, response.cupoMaximo());
        assertEquals(expectedCutoff, response.minutosCorte());
    }

    private void thenResponseIsInactive(TimeSlotDetailsResponse response) {
        assertFalse(response.activo());
    }

    private void thenResponseContainsOneInactiveSlot(List<TimeSlotDetailsResponse> response) {
        assertEquals(1, response.size());
        assertFalse(response.get(0).activo());
    }

    private void thenThrowsOverlapException(Executable execution) {
        assertThrows(TimeSlotOverlapException.class, execution);
    }

    private void thenThrowsInUseException(Executable execution) {
        assertThrows(TimeSlotInUseException.class, execution);
    }

    private void thenSlotWasNotSaved() {
        verify(franjaHorariaRepository, never()).save(any(FranjaHoraria.class));
    }

    private void thenSlotWasSaved() {
        verify(franjaHorariaRepository).save(any(FranjaHoraria.class));
    }

    private void thenFuturePurchasesWereNotChecked() {
        verify(purchaseRepository, never())
                .existsActiveFutureScheduledByPickupSlotId(any(), any(), any());
    }

    private void thenAdministrativeQueryWasUsed() {
        verify(franjaHorariaRepository).findAllByColegioIdIncludingInactive(COLEGIO_ID);
    }
}
