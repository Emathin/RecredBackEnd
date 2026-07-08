package com.recred.backend.unit.application;

import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTimeSlotsBySchoolRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTimeSlotsBySchoolTest {

    @Mock
    private FranjaHorariaRepository repository;

    private GetTimeSlotsBySchool useCase;

    private UUID schoolId;
    private FranjaHoraria morningSlot;
    private FranjaHoraria afternoonSlot;
    private List<GetTimeSlotsBySchoolResponse> response;

    @BeforeEach
    void setUp() {
        useCase = new GetTimeSlotsBySchool(repository);
    }

    @Test
    void givenSchoolWithTimeSlots_whenGettingTimeSlots_thenReturnsSchoolTimeSlots() {
        givenTimeSlotsScenario();
        givenSchoolHasTimeSlots();

        whenGettingTimeSlots();

        thenTimeSlotsAreReturned();
        thenTimeSlotsWereRequestedForSchool();
    }

    @Test
    void givenSchoolWithoutTimeSlots_whenGettingTimeSlots_thenReturnsEmptyList() {
        givenTimeSlotsScenario();
        givenSchoolHasNoTimeSlots();

        whenGettingTimeSlots();

        thenNoTimeSlotsAreReturned();
        thenTimeSlotsWereRequestedForSchool();
    }

    private void givenTimeSlotsScenario() {
        schoolId = UUID.randomUUID();
    }

    private void givenSchoolHasTimeSlots() {
        morningSlot = timeSlot("Turno Manana", LocalTime.of(8, 0), LocalTime.of(12, 0), true);
        afternoonSlot = timeSlot("Turno Tarde", LocalTime.of(13, 0), LocalTime.of(17, 0), false);
        when(repository.findAllByColegioId(schoolId))
                .thenReturn(List.of(morningSlot, afternoonSlot));
    }

    private void givenSchoolHasNoTimeSlots() {
        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of());
    }

    private void whenGettingTimeSlots() {
        response = useCase.execute(new GetTimeSlotsBySchoolRequest(schoolId));
    }

    private void thenTimeSlotsAreReturned() {
        assertEquals(2, response.size());
        GetTimeSlotsBySchoolResponse firstSlot = response.get(0);
        assertEquals(morningSlot.getId(), firstSlot.id());
        assertEquals(morningSlot.getColegioId(), firstSlot.colegioId());
        assertEquals(morningSlot.getDescripcion(), firstSlot.descripcion());
        assertEquals(morningSlot.getHoraInicio(), firstSlot.horaInicio());
        assertEquals(morningSlot.getHoraFin(), firstSlot.horaFin());
        assertEquals(morningSlot.isActivo(), firstSlot.activo());
    }

    private void thenNoTimeSlotsAreReturned() {
        assertEquals(0, response.size());
    }

    private void thenTimeSlotsWereRequestedForSchool() {
        verify(repository).findAllByColegioId(schoolId);
    }

    private FranjaHoraria timeSlot(
            String description,
            LocalTime start,
            LocalTime end,
            boolean active
    ) {
        return new FranjaHoraria(UUID.randomUUID(), schoolId, description, start, end, active);
    }
}
