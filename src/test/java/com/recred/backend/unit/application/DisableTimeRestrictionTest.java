package com.recred.backend.unit.application;

import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.DisableTimeRestrictionRequest;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
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
    private RestriccionHorariaRepository repository;

    @InjectMocks
    private DisableTimeRestriction disableTimeRestriction;

    @Test
    void execute_DesactivaLaRestriccion_CuandoExiste() {
        UUID restrictionId = UUID.randomUUID();
        RestriccionHoraria restriccion = new RestriccionHoraria();
        restriccion.setActiva(true);

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccion));

        disableTimeRestriction.execute(new DisableTimeRestrictionRequest(restrictionId));

        assertFalse(restriccion.isActiva());
        verify(repository).save(restriccion);
    }

    @Test
    void execute_LanzaExcepcion_CuandoNoExiste() {
        UUID restrictionId = UUID.randomUUID();
        when(repository.findById(restrictionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> disableTimeRestriction.execute(new DisableTimeRestrictionRequest(restrictionId)));

        assertEquals("Time restriction not found", exception.getMessage());
    }
}
