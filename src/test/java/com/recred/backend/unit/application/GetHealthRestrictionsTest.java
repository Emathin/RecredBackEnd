package com.recred.backend.unit.application;

import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetHealthRestrictions useCase;

    private UUID alumnoId;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        alumno = new Alumno();
        alumno.setId(alumnoId);
    }

    @Test
    void ejecutar_CuandoAlumnoExiste_DeberiaRetornarRestricciones() {
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(UUID.randomUUID());
        alumno.getRestriccionesSalud().add(clasificacion);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        Set<ClasificacionSalud> resultado = useCase.ejecutar(alumnoId);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.contains(clasificacion));
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId);
        });

        assertEquals("El alumno con ID " + alumnoId + " no existe.", exception.getMessage());
    }
}
