package com.recred.backend.unit.application;

import com.recred.backend.application.GetBuffetTimeSlots;
import com.recred.backend.application.GetBuffetTimeSlotsRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
import com.recred.backend.domain.exceptions.BuffetNotFoundException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetBuffetTimeSlotsTest {

    private BuffetRepository buffetRepository;
    private FranjaHorariaRepository timeSlotRepository;
    private GetBuffetTimeSlots useCase;

    private UUID buffetId;
    private UUID schoolId;
    private FranjaHoraria timeSlot;
    private List<GetTimeSlotsBySchoolResponse> response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        buffetRepository = mock(BuffetRepository.class);
        timeSlotRepository = mock(FranjaHorariaRepository.class);
        useCase = new GetBuffetTimeSlots(buffetRepository, timeSlotRepository);
    }

    @Test
    void givenBuffetWithSchool_whenGettingBuffetTimeSlots_thenReturnsSchoolTimeSlots() {
        givenBuffetTimeSlotScenario();
        givenBuffetHasSchool();
        givenSchoolHasTimeSlots();

        whenGettingBuffetTimeSlots();

        thenTimeSlotIsReturned();
        thenSchoolTimeSlotsWereRequested();
    }

    @Test
    void givenMissingBuffet_whenGettingBuffetTimeSlots_thenFailsWithoutSearchingSlots() {
        givenBuffetTimeSlotScenario();
        givenBuffetDoesNotExist();

        whenGettingBuffetTimeSlotsFails();

        thenFailsWith(BuffetNotFoundException.class);
        thenSchoolTimeSlotsWereNotRequested();
    }

    @Test
    void givenBuffetWithoutSchool_whenGettingBuffetTimeSlots_thenFailsWithoutSearchingSlots() {
        givenBuffetTimeSlotScenario();
        givenBuffetHasNoSchool();

        whenGettingBuffetTimeSlotsFails();

        thenFailsWith(PickupTimeSlotNotFoundException.class);
        thenSchoolTimeSlotsWereNotRequested();
    }

    private void givenBuffetTimeSlotScenario() {
        buffetId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        timeSlot = new FranjaHoraria(
                UUID.randomUUID(),
                schoolId,
                "Primer recreo",
                LocalTime.of(9, 30),
                LocalTime.of(9, 45),
                true
        );
    }

    private void givenBuffetHasSchool() {
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet(schoolId)));
    }

    private void givenBuffetDoesNotExist() {
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.empty());
    }

    private void givenBuffetHasNoSchool() {
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(new Buffet()));
    }

    private void givenSchoolHasTimeSlots() {
        when(timeSlotRepository.findAllByColegioId(schoolId)).thenReturn(List.of(timeSlot));
    }

    private void whenGettingBuffetTimeSlots() {
        response = useCase.execute(new GetBuffetTimeSlotsRequest(buffetId));
    }

    private void whenGettingBuffetTimeSlotsFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetBuffetTimeSlotsRequest(buffetId)));
    }

    private void thenTimeSlotIsReturned() {
        assertEquals(1, response.size());
        assertEquals(timeSlot.getId(), response.get(0).id());
        assertEquals(timeSlot.getDescripcion(), response.get(0).descripcion());
    }

    private void thenSchoolTimeSlotsWereRequested() {
        verify(timeSlotRepository).findAllByColegioId(schoolId);
    }

    private void thenFailsWith(Class<? extends RuntimeException> expectedType) {
        assertTrue(expectedType.isInstance(thrownException));
    }

    private void thenSchoolTimeSlotsWereNotRequested() {
        verifyNoInteractions(timeSlotRepository);
    }

    private Buffet buffet(UUID schoolId) {
        Colegio school = new Colegio();
        school.setId(schoolId);
        Buffet buffet = new Buffet();
        buffet.setColegio(school);
        return buffet;
    }
}
