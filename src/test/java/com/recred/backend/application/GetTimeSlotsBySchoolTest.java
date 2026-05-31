package com.recred.backend.application;

import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.repositories.TimeBadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTimeSlotsBySchoolTest {

    @Mock
    private TimeBadgeRepository repository;

    @InjectMocks
    private GetTimeSlotsBySchool getTimeSlotsBySchool;

    @Test
    void execute_DevuelveListaDeFranjas() {
        UUID colegioId = UUID.randomUUID();
        List<TimeBadge> franjasEsperadas = List.of(new TimeBadge(), new TimeBadge());

        when(repository.findAllByColegioId(colegioId)).thenReturn(franjasEsperadas);

        List<TimeBadge> resultado = getTimeSlotsBySchool.execute(colegioId);

        assertEquals(2, resultado.size());
        verify(repository).findAllByColegioId(colegioId);
    }
}