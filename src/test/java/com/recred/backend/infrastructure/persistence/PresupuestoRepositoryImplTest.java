package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoRepositoryImplTest {

    @Mock
    private SpringDataPresupuestoRepository springDataRepository;

    @InjectMocks
    private PresupuestoRepositoryImpl repositoryImpl;

    private UUID studentId;
    private PresupuestoAlumnoEntity entityMock;
    private PresupuestoAlumno domainMock;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        entityMock = new PresupuestoAlumnoEntity();
        entityMock.setId(UUID.randomUUID());

        domainMock = new PresupuestoAlumno();
        domainMock.setId(entityMock.getId());
    }

    @Test
    void findActivoByAlumnoId_SiExiste_RetornaDominioMapeado() {

        when(springDataRepository.findByAlumnoEntityIdAndActivoTrue(studentId)).thenReturn(Optional.of(entityMock));

        Optional<PresupuestoAlumno> result = repositoryImpl.findActivoByAlumnoId(studentId);

        assertTrue(result.isPresent());

        assertEquals(entityMock.getId(), result.get().getId());
        verify(springDataRepository).findByAlumnoEntityIdAndActivoTrue(studentId);
    }

    @Test
    void findActivoByAlumnoId_SiNoExiste_RetornaEmpty() {
        when(springDataRepository.findByAlumnoEntityIdAndActivoTrue(studentId)).thenReturn(Optional.empty());

        Optional<PresupuestoAlumno> result = repositoryImpl.findActivoByAlumnoId(studentId);

        assertTrue(result.isEmpty());
        verify(springDataRepository).findByAlumnoEntityIdAndActivoTrue(studentId);
    }

    @Test
    void save_ConvierteADominio_Guarda_YRetornaDominio() {

        when(springDataRepository.save(any(PresupuestoAlumnoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        PresupuestoAlumno resultado = repositoryImpl.save(domainMock);

        assertNotNull(resultado);
        assertEquals(domainMock.getId(), resultado.getId());

        verify(springDataRepository).save(any(PresupuestoAlumnoEntity.class));
    }
}