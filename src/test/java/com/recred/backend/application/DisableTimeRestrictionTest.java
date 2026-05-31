package com.recred.backend.application;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisableTimeRestrictionTest {

    @Mock
    private TimeRestrictionRepository repository;

    @InjectMocks
    private DisableTimeRestriction disableTimeRestriction;

    @Test
    void execute_DesactivaLaRestriccion_CuandoExiste() {
        UUID restrictionId = UUID.randomUUID();
        TimeRestriction restriccion = new TimeRestriction();
        restriccion.setActive(true);

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccion));

        disableTimeRestriction.execute(restrictionId);

        assertFalse(restriccion.isActive());
        verify(repository).save(restriccion);
    }

    @Test
    void execute_LanzaExcepcion_CuandoNoExiste() {
        UUID restrictionId = UUID.randomUUID();
        when(repository.findById(restrictionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> disableTimeRestriction.execute(restrictionId));

        assertEquals("Time restriction not found", exception.getMessage());
    }
}