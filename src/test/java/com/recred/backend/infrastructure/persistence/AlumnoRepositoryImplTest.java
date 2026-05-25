package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoRepositoryImplTest {

    @Mock
    private SpringDataAlumnoRepository springDataRepository;

    @InjectMocks
    private AlumnoRepositoryImpl alumnoRepositoryImpl;

    @Captor
    private ArgumentCaptor<AlumnoEntity> entityCaptor;

    private UUID alumnoId;
    private UUID clasificacionId;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();
    }

    @Test
    void save_DeberiaMapearADominioYLlamarAlSpringData() {
        Alumno alumnoDominio = new Alumno();
        alumnoDominio.setId(alumnoId);

        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescripcion("Celíaco");
        alumnoDominio.agregarRestriccionSalud(clasificacion);

        alumnoRepositoryImpl.save(alumnoDominio);

        verify(springDataRepository).save(entityCaptor.capture());
        AlumnoEntity entidadAtrapada = entityCaptor.getValue();

        assertNotNull(entidadAtrapada, "La entidad generada no debe ser nula");
        assertEquals(alumnoId, entidadAtrapada.getId());

        assertNotNull(entidadAtrapada.getRestriccionesSalud(), "La lista de entidades no debe ser nula");
        assertEquals(1, entidadAtrapada.getRestriccionesSalud().size(), "Debe haber mapeado 1 restricción");
        assertEquals(clasificacionId, entidadAtrapada.getRestriccionesSalud().iterator().next().getId(), "El ID de la restricción debe coincidir");
    }

    @Test
    void findById_CuandoExiste_DeberiaMapearADominioYRetornar() {
        AlumnoEntity entidadFalsa = new AlumnoEntity();
        entidadFalsa.setId(alumnoId);

        ClasificacionSaludEntity clasificacionEntity = new ClasificacionSaludEntity();
        clasificacionEntity.setId(clasificacionId);
        clasificacionEntity.setDescripcion("Diabético");
        entidadFalsa.setRestriccionesSalud(Set.of(clasificacionEntity));

        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.of(entidadFalsa));

        Optional<Alumno> resultado = alumnoRepositoryImpl.findById(alumnoId);

        assertTrue(resultado.isPresent(), "El alumno debe estar presente");
        Alumno alumnoMapeado = resultado.get();

        assertEquals(alumnoId, alumnoMapeado.getId());
        assertNotNull(alumnoMapeado.getRestriccionesSalud());
        assertEquals(1, alumnoMapeado.getRestriccionesSalud().size());

        ClasificacionSalud restriccionMapeada = alumnoMapeado.getRestriccionesSalud().iterator().next();
        assertEquals(clasificacionId, restriccionMapeada.getId());
        assertEquals("Diabético", restriccionMapeada.getDescripcion());

        verify(springDataRepository).findById(alumnoId);
    }

    @Test
    void findById_CuandoNoExiste_DeberiaRetornarVacio() {
        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Optional<Alumno> resultado = alumnoRepositoryImpl.findById(alumnoId);

        assertTrue(resultado.isEmpty());
        verify(springDataRepository).findById(alumnoId);
    }
}