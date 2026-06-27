package com.recred.backend.unit.application;

import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTimeSlotsBySchoolRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private GetTimeSlotsBySchool getTimeSlotsBySchool;

    @Test
    void execute_DevuelveListaDeFranjas() {
        UUID colegioId = UUID.randomUUID();
        FranjaHoraria slot1 = new FranjaHoraria(UUID.randomUUID(), colegioId, "Turno Mañana",
                LocalTime.of(8, 0), LocalTime.of(12, 0), true);
        FranjaHoraria slot2 = new FranjaHoraria(UUID.randomUUID(), colegioId, "Turno Tarde",
                LocalTime.of(13, 0), LocalTime.of(17, 0), false);
        List<FranjaHoraria> franjasEsperadas = List.of(slot1, slot2);

        when(repository.findAllByColegioId(colegioId)).thenReturn(franjasEsperadas);

        List<GetTimeSlotsBySchoolResponse> resultado = getTimeSlotsBySchool.execute(new GetTimeSlotsBySchoolRequest(colegioId));

        assertEquals(2, resultado.size());
        
        GetTimeSlotsBySchoolResponse res1 = resultado.get(0);
        assertEquals(slot1.getId(), res1.id());
        assertEquals(slot1.getColegioId(), res1.colegioId());
        assertEquals(slot1.getDescripcion(), res1.descripcion());
        assertEquals(slot1.getHoraInicio(), res1.horaInicio());
        assertEquals(slot1.getHoraFin(), res1.horaFin());
        assertEquals(slot1.isActivo(), res1.activo());

        verify(repository).findAllByColegioId(colegioId);
    }

    @Test
    void execute_CuandoNoHayFranjas_RetornaListaVacia() {
        UUID colegioId = UUID.randomUUID();
        when(repository.findAllByColegioId(colegioId)).thenReturn(List.of());

        List<GetTimeSlotsBySchoolResponse> resultado =
                getTimeSlotsBySchool.execute(new GetTimeSlotsBySchoolRequest(colegioId));

        assertEquals(0, resultado.size());
        verify(repository).findAllByColegioId(colegioId);
    }
}
