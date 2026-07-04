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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeSlotManagementUseCasesTest {

    private final FranjaHorariaRepository franjaHorariaRepository =
            mock(FranjaHorariaRepository.class);
    private final ColegioRepository colegioRepository = mock(ColegioRepository.class);
    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void create_ShouldSaveActiveTimeSlotForActiveSchool() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        CreateTimeSlot useCase = new CreateTimeSlot(
                franjaHorariaRepository,
                colegioRepository);
        CreateTimeSlotRequest request = new CreateTimeSlotRequest(
                colegioId,
                " Primer recreo ",
                LocalTime.of(9, 0),
                LocalTime.of(9, 15),
                30,
                null
        );

        when(colegioRepository.existsActiveById(colegioId)).thenReturn(true);
        when(franjaHorariaRepository.existsActiveOverlap(
                eq(colegioId),
                eq(null),
                eq(LocalTime.of(9, 0)),
                eq(LocalTime.of(9, 15)))).thenReturn(false);
        when(franjaHorariaRepository.save(any(FranjaHoraria.class))).thenAnswer(invocation -> {
            FranjaHoraria slot = invocation.getArgument(0);
            slot.setId(slotId);
            return slot;
        });

        TimeSlotDetailsResponse response = useCase.execute(request);

        assertEquals(slotId, response.id());
        assertEquals("Primer recreo", response.descripcion());
        assertEquals(0, response.minutosCorte());
        assertTrue(response.activo());
    }

    @Test
    void create_WhenOverlapsActiveSlot_ShouldThrowException() {
        UUID colegioId = UUID.randomUUID();
        CreateTimeSlot useCase = new CreateTimeSlot(
                franjaHorariaRepository,
                colegioRepository);
        CreateTimeSlotRequest request = new CreateTimeSlotRequest(
                colegioId,
                "Primer recreo",
                LocalTime.of(9, 0),
                LocalTime.of(9, 15),
                null,
                0
        );

        when(colegioRepository.existsActiveById(colegioId)).thenReturn(true);
        when(franjaHorariaRepository.existsActiveOverlap(
                eq(colegioId),
                eq(null),
                eq(LocalTime.of(9, 0)),
                eq(LocalTime.of(9, 15)))).thenReturn(true);

        Executable execution = () -> useCase.execute(request);

        assertThrows(TimeSlotOverlapException.class, execution);
        verify(franjaHorariaRepository, never()).save(any(FranjaHoraria.class));
    }

    @Test
    void update_WhenStructuralDataChangesAndHasFuturePurchases_ShouldThrowException() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UpdateTimeSlot useCase = updateUseCase();
        FranjaHoraria existing = slot(slotId, colegioId, "Primer recreo",
                LocalTime.of(9, 0), LocalTime.of(9, 15), true);
        UpdateTimeSlotRequest request = new UpdateTimeSlotRequest(
                colegioId,
                slotId,
                "Segundo recreo",
                LocalTime.of(10, 0),
                LocalTime.of(10, 15),
                40,
                5
        );

        when(colegioRepository.existsActiveById(colegioId)).thenReturn(true);
        when(franjaHorariaRepository.findByIdAndColegioId(slotId, colegioId))
                .thenReturn(Optional.of(existing));
        when(purchaseRepository.existsActiveFutureScheduledByPickupSlotId(
                eq(slotId),
                eq(LocalDate.of(2026, 7, 2)),
                any())).thenReturn(true);

        Executable execution = () -> useCase.execute(request);

        assertThrows(TimeSlotInUseException.class, execution);
        verify(franjaHorariaRepository, never()).save(any(FranjaHoraria.class));
    }

    @Test
    void update_WhenOnlyRulesChange_ShouldSaveWithoutCheckingFuturePurchases() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UpdateTimeSlot useCase = updateUseCase();
        FranjaHoraria existing = slot(slotId, colegioId, "Primer recreo",
                LocalTime.of(9, 0), LocalTime.of(9, 15), true);
        UpdateTimeSlotRequest request = new UpdateTimeSlotRequest(
                colegioId,
                slotId,
                "Primer recreo",
                LocalTime.of(9, 0),
                LocalTime.of(9, 15),
                40,
                5
        );

        when(colegioRepository.existsActiveById(colegioId)).thenReturn(true);
        when(franjaHorariaRepository.findByIdAndColegioId(slotId, colegioId))
                .thenReturn(Optional.of(existing));
        when(franjaHorariaRepository.existsActiveOverlap(
                colegioId,
                slotId,
                LocalTime.of(9, 0),
                LocalTime.of(9, 15))).thenReturn(false);
        when(franjaHorariaRepository.save(existing)).thenReturn(existing);

        TimeSlotDetailsResponse response = useCase.execute(request);

        assertEquals(40, response.cupoMaximo());
        assertEquals(5, response.minutosCorte());
        verify(purchaseRepository, never())
                .existsActiveFutureScheduledByPickupSlotId(any(), any(), any());
    }

    @Test
    void deactivate_ShouldSaveInactiveSlot() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        DeactivateTimeSlot useCase = new DeactivateTimeSlot(franjaHorariaRepository);
        FranjaHoraria existing = slot(slotId, colegioId, "Primer recreo",
                LocalTime.of(9, 0), LocalTime.of(9, 15), true);
        DeactivateTimeSlotRequest request = new DeactivateTimeSlotRequest(colegioId, slotId);

        when(franjaHorariaRepository.findByIdAndColegioId(slotId, colegioId))
                .thenReturn(Optional.of(existing));
        when(franjaHorariaRepository.save(existing)).thenReturn(existing);

        TimeSlotDetailsResponse response = useCase.execute(request);

        assertFalse(response.activo());
        verify(franjaHorariaRepository).save(existing);
    }

    @Test
    void reactivate_WhenOverlapsActiveSlot_ShouldThrowException() {
        UUID colegioId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ReactivateTimeSlot useCase = new ReactivateTimeSlot(franjaHorariaRepository);
        FranjaHoraria existing = slot(slotId, colegioId, "Primer recreo",
                LocalTime.of(9, 0), LocalTime.of(9, 15), false);
        ReactivateTimeSlotRequest request = new ReactivateTimeSlotRequest(colegioId, slotId);

        when(franjaHorariaRepository.findByIdAndColegioId(slotId, colegioId))
                .thenReturn(Optional.of(existing));
        when(franjaHorariaRepository.existsActiveOverlap(
                colegioId,
                slotId,
                LocalTime.of(9, 0),
                LocalTime.of(9, 15))).thenReturn(true);

        Executable execution = () -> useCase.execute(request);

        assertThrows(TimeSlotOverlapException.class, execution);
        verify(franjaHorariaRepository, never()).save(any(FranjaHoraria.class));
    }

    @Test
    void list_WhenIncludesInactive_ShouldUseAdministrativeQuery() {
        UUID colegioId = UUID.randomUUID();
        ListSchoolTimeSlots useCase = new ListSchoolTimeSlots(franjaHorariaRepository);
        FranjaHoraria inactive = slot(UUID.randomUUID(), colegioId, "Salida",
                LocalTime.of(12, 0), LocalTime.of(12, 15), false);
        ListSchoolTimeSlotsRequest request = new ListSchoolTimeSlotsRequest(colegioId, true);

        when(franjaHorariaRepository.findAllByColegioIdIncludingInactive(colegioId))
                .thenReturn(List.of(inactive));

        List<TimeSlotDetailsResponse> response = useCase.execute(request);

        assertEquals(1, response.size());
        assertFalse(response.get(0).activo());
        verify(franjaHorariaRepository).findAllByColegioIdIncludingInactive(colegioId);
    }

    private UpdateTimeSlot updateUseCase() {
        return new UpdateTimeSlot(
                franjaHorariaRepository,
                colegioRepository,
                purchaseRepository,
                clock);
    }

    private FranjaHoraria slot(UUID slotId, UUID colegioId, String descripcion,
                               LocalTime horaInicio, LocalTime horaFin, boolean activo) {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(slotId);
        slot.setColegioId(colegioId);
        slot.setDescripcion(descripcion);
        slot.setHoraInicio(horaInicio);
        slot.setHoraFin(horaFin);
        slot.setActivo(activo);
        return slot;
    }
}
