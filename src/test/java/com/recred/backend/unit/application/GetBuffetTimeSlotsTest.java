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
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetBuffetTimeSlotsTest {

    private final BuffetRepository buffetRepository = mock(BuffetRepository.class);
    private final FranjaHorariaRepository franjaHorariaRepository =
            mock(FranjaHorariaRepository.class);
    private final GetBuffetTimeSlots useCase = new GetBuffetTimeSlots(
            buffetRepository,
            franjaHorariaRepository
    );

    @Test
    void execute_WhenBuffetExists_ShouldReturnSchoolTimeSlots() {
        UUID buffetId = UUID.randomUUID();
        UUID colegioId = UUID.randomUUID();
        Buffet buffet = buffet(colegioId);
        FranjaHoraria slot = new FranjaHoraria(
                UUID.randomUUID(),
                colegioId,
                "Primer recreo",
                LocalTime.of(9, 30),
                LocalTime.of(9, 45),
                true
        );

        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));
        when(franjaHorariaRepository.findAllByColegioId(colegioId)).thenReturn(List.of(slot));

        List<GetTimeSlotsBySchoolResponse> result = useCase.execute(
                new GetBuffetTimeSlotsRequest(buffetId));

        assertEquals(slot.getId(), result.get(0).id());
        assertEquals(slot.getDescripcion(), result.get(0).descripcion());
        verify(franjaHorariaRepository).findAllByColegioId(colegioId);
    }

    @Test
    void execute_WhenBuffetDoesNotExist_ShouldThrowException() {
        UUID buffetId = UUID.randomUUID();

        when(buffetRepository.findById(buffetId)).thenReturn(Optional.empty());

        assertThrows(BuffetNotFoundException.class,
                () -> useCase.execute(new GetBuffetTimeSlotsRequest(buffetId)));
    }

    @Test
    void execute_WhenBuffetHasNoSchool_ShouldThrowException() {
        UUID buffetId = UUID.randomUUID();
        Buffet buffet = new Buffet();

        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));

        assertThrows(PickupTimeSlotNotFoundException.class,
                () -> useCase.execute(new GetBuffetTimeSlotsRequest(buffetId)));
    }

    private Buffet buffet(UUID colegioId) {
        Colegio colegio = new Colegio();
        colegio.setId(colegioId);
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        return buffet;
    }
}
