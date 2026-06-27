package com.recred.backend.unit.application;

import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import com.recred.backend.application.GetBuffetByStudentResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetBuffetByStudentTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private BuffetRepository buffetRepository;

    @InjectMocks
    private GetBuffetByStudent getBuffetByStudent;

    @Test
    void dadoAlumnoExistenteYBuffetActivo_cuandoExecute_entoncesRetornaBuffet() {
        UUID alumnoId = UUID.randomUUID();
        Buffet buffet = new Buffet();
        buffet.setId(UUID.randomUUID());
        buffet.setNombre("Buffet Central");

        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(buffetRepository.findByAlumnoId(alumnoId)).thenReturn(Optional.of(buffet));

        Optional<GetBuffetByStudentResponse> resultado = getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId));

        assertTrue(resultado.isPresent());
        assertEquals("Buffet Central", resultado.get().nombre());
        verify(alumnoRepository, times(1)).existsById(alumnoId);
        verify(buffetRepository, times(1)).findByAlumnoId(alumnoId);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaAlumnoNoEncontradoException() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(false);

        assertThrows(AlumnoNoEncontradoException.class, () ->
                getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId))
        );

        verify(alumnoRepository, times(1)).existsById(alumnoId);
        verifyNoInteractions(buffetRepository);
    }

    @Test
    void dadoAlumnoExistentePeroSinBuffet_cuandoExecute_entoncesRetornaVacio() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(buffetRepository.findByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        Optional<GetBuffetByStudentResponse> resultado = getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId));

        assertFalse(resultado.isPresent());
        verify(alumnoRepository, times(1)).existsById(alumnoId);
        verify(buffetRepository, times(1)).findByAlumnoId(alumnoId);
    }
}
