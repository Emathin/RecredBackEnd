package com.recred.backend.unit.application;

import com.recred.backend.application.GetActiveLevels;
import com.recred.backend.application.GetActiveLevelsRequest;
import com.recred.backend.application.GetActiveLevelsResponse;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.NivelRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetActiveLevelsTest {

    private final NivelRepository repository = mock(NivelRepository.class);
    private final GetActiveLevels useCase = new GetActiveLevels(repository);

    @Test
    void execute_ShouldReturnActiveLevels() {
        UUID levelId = UUID.randomUUID();
        givenActiveLevels(level(levelId, "Primario"));

        GetActiveLevelsResponse response = whenGettingActiveLevels();

        assertEquals(1, response.levels().size());
        assertEquals(levelId, response.levels().get(0).id());
        assertEquals("Primario", response.levels().get(0).descripcion());
        verify(repository).findAllActive();
    }

    private void givenActiveLevels(Nivel... levels) {
        when(repository.findAllActive()).thenReturn(List.of(levels));
    }

    private GetActiveLevelsResponse whenGettingActiveLevels() {
        return useCase.execute(new GetActiveLevelsRequest());
    }

    private Nivel level(UUID id, String descripcion) {
        Nivel level = new Nivel();
        level.setId(id);
        level.setDescripcion(descripcion);
        level.setActivo(true);
        return level;
    }
}
