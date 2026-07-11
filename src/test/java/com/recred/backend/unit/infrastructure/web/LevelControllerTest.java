package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetActiveLevels;
import com.recred.backend.application.GetActiveLevelsRequest;
import com.recred.backend.application.GetActiveLevelsResponse;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.infrastructure.web.LevelController;
import com.recred.backend.infrastructure.web.LevelResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LevelControllerTest {

    private final GetActiveLevels getActiveLevels = mock(GetActiveLevels.class);
    private final LevelController controller = new LevelController(getActiveLevels);

    @Test
    void getLevelsShouldReturnActiveLevels() {
        UUID levelId = UUID.randomUUID();
        givenActiveLevels(level(levelId, "Secundario"));

        ResponseEntity<List<LevelResponseDTO>> response = whenGettingLevels();

        thenActiveLevelsAreReturned(response, levelId);
    }

    private void givenActiveLevels(Nivel... levels) {
        when(getActiveLevels.execute(new GetActiveLevelsRequest()))
                .thenReturn(GetActiveLevelsResponse.fromDomain(List.of(levels)));
    }

    private ResponseEntity<List<LevelResponseDTO>> whenGettingLevels() {
        return controller.getLevels();
    }

    private void thenActiveLevelsAreReturned(ResponseEntity<List<LevelResponseDTO>> response, UUID expectedLevelId) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        
        LevelResponseDTO level = response.getBody().get(0);
        assertEquals(expectedLevelId, level.id());
        assertEquals("Secundario", level.descripcion());
        verify(getActiveLevels).execute(new GetActiveLevelsRequest());
    }

    private Nivel level(UUID id, String descripcion) {
        Nivel level = new Nivel();
        level.setId(id);
        level.setDescripcion(descripcion);
        level.setActivo(true);
        return level;
    }
}
