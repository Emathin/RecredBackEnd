package com.recred.backend.unit.application;

import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisableTimeRestrictionTest {

    @Mock
    private RestriccionHorariaRepository repository;

    @InjectMocks
    private DisableTimeRestriction disableTimeRestriction;

    @Test
    void executeWhenRestrictionExistsShouldDeactivateAndSave() {
        UUID restrictionId = UUID.randomUUID();
        RestriccionHoraria restriction = new RestriccionHoraria();
        restriction.setActiva(true);

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriction));

        disableTimeRestriction.execute(restrictionId);

        assertFalse(restriction.isActiva());
        verify(repository).save(restriction);
    }

    @Test
    void executeWhenRestrictionDoesNotExistShouldThrowException() {
        UUID restrictionId = UUID.randomUUID();
        when(repository.findById(restrictionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> disableTimeRestriction.execute(restrictionId));

        assertEquals("Time restriction not found", exception.getMessage());
    }
}
