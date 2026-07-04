package com.recred.backend.unit.application;

import com.recred.backend.application.ManualProductUnlock;
import com.recred.backend.application.ManualProductUnlockRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualProductUnlockTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private ManualProductUnlock manualProductUnlock;

    @Test
    void dadoAlumnoValido_cuandoExecute_entoncesDesbloqueaYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        ManualProductUnlockRequest request = new ManualProductUnlockRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        manualProductUnlock.execute(request);

        verify(alumnoRepository).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ManualProductUnlockRequest request = new ManualProductUnlockRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> manualProductUnlock.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(alumnoRepository, never()).save(any());
    }
}
