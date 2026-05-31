package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Student;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.HealthClassificationEntity;
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
class StudentRepositoryImplTest {

    @Mock
    private SpringDataAlumnoRepository springDataRepository;

    @InjectMocks
    private StudentRepositoryImpl alumnoRepositoryImpl;

    @Captor
    private ArgumentCaptor<StudentEntity> entityCaptor;

    private UUID alumnoId;
    private UUID clasificacionId;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();
    }

    @Test
    void save_DeberiaMapearADominioYLlamarAlSpringData() {
        Student studentDominio = new Student();
        studentDominio.setId(alumnoId);

        HealthClassification clasificacion = new HealthClassification();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescription("Celíaco");
        studentDominio.addHealthRestriction(clasificacion);

        alumnoRepositoryImpl.save(studentDominio);

        verify(springDataRepository).save(entityCaptor.capture());
        StudentEntity entidadAtrapada = entityCaptor.getValue();

        assertNotNull(entidadAtrapada, "La entidad generada no debe ser nula");
        assertEquals(alumnoId, entidadAtrapada.getId());

        assertNotNull(entidadAtrapada.getHealthRestrictions(), "La lista de entidades no debe ser nula");
        assertEquals(1, entidadAtrapada.getHealthRestrictions().size(), "Debe haber mapeado 1 restricción");
        assertEquals(clasificacionId, entidadAtrapada.getHealthRestrictions().iterator().next().getId(), "El ID de la restricción debe coincidir");
    }

    @Test
    void findById_CuandoExiste_DeberiaMapearADominioYRetornar() {
        StudentEntity entidadFalsa = new StudentEntity();
        entidadFalsa.setId(alumnoId);

        HealthClassificationEntity clasificacionEntity = new HealthClassificationEntity();
        clasificacionEntity.setId(clasificacionId);
        clasificacionEntity.setDescription("Diabético");
        entidadFalsa.setHealthRestrictions(Set.of(clasificacionEntity));

        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.of(entidadFalsa));

        Optional<Student> resultado = alumnoRepositoryImpl.findById(alumnoId);

        assertTrue(resultado.isPresent(), "El alumno debe estar presente");
        Student studentMapeado = resultado.get();

        assertEquals(alumnoId, studentMapeado.getId());
        assertNotNull(studentMapeado.getHealthRestrictions());
        assertEquals(1, studentMapeado.getHealthRestrictions().size());

        HealthClassification restriccionMapeada = studentMapeado.getHealthRestrictions().iterator().next();
        assertEquals(clasificacionId, restriccionMapeada.getId());
        assertEquals("Diabético", restriccionMapeada.getDescription());

        verify(springDataRepository).findById(alumnoId);
    }

    @Test
    void findById_CuandoNoExiste_DeberiaRetornarVacio() {
        when(springDataRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Optional<Student> resultado = alumnoRepositoryImpl.findById(alumnoId);

        assertTrue(resultado.isEmpty());
        verify(springDataRepository).findById(alumnoId);
    }
}