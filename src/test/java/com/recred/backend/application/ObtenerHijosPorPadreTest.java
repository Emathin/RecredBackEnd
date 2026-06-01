package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class ObtenerHijosPorPadreTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private ObtenerHijosPorPadre obtenerHijosPorPadre;

    @Test
    void dadoUnTutorExistente_cuandoSeEjecuta_entoncesRetornaSusHijos() {
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);

        Alumno hijo1 = new Alumno();
        hijo1.setId(UUID.randomUUID());
        tutor.addHijo(hijo1);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        Set<Alumno> resultado = obtenerHijosPorPadre.execute(tutorId);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(tutorRepository, times(1)).findById(tutorId);
    }

    @Test
    void dadoUnTutorInexistente_cuandoSeEjecuta_entoncesLanzaExcepcion() {
        UUID tutorId = UUID.randomUUID();

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            obtenerHijosPorPadre.execute(tutorId);
        });

        assertEquals("Tutor no encontrado con ID: " + tutorId, exception.getMessage());
        verify(tutorRepository, times(1)).findById(tutorId);
    }
}