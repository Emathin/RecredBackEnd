package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.infrastructure.entities.HealthClassificationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthClassificationRepositoryImplTest {

    @Mock
    private SpringDataClasificacionSaludRepository springDataRepository;

    @InjectMocks
    private HealthClassificationRepositoryImpl repositoryImpl;

    private UUID id1;
    private UUID id2;

    @BeforeEach
    void setUp() {
        id1 = UUID.randomUUID();
        id2 = UUID.randomUUID();
    }

    @Test
    void findAllById_DeberiaMapearListaDeEntidadesADominioYRetornar() {
        HealthClassificationEntity entity1 = new HealthClassificationEntity();
        entity1.setId(id1);
        entity1.setDescription("Celíaco");

        HealthClassificationEntity entity2 = new HealthClassificationEntity();
        entity2.setId(id2);
        entity2.setDescription("Diabético");

        List<UUID> idsBuscados = List.of(id1, id2);
        when(springDataRepository.findAllById(idsBuscados)).thenReturn(List.of(entity1, entity2));

        List<HealthClassification> resultado = repositoryImpl.findAllById(idsBuscados);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());

        assertEquals(id1, resultado.get(0).getId());
        assertEquals("Celíaco", resultado.get(0).getDescription());

        assertEquals(id2, resultado.get(1).getId());
        assertEquals("Diabético", resultado.get(1).getDescription());

        verify(springDataRepository).findAllById(idsBuscados);
    }

    @Test
    void findById_CuandoExiste_DeberiaMapearYRetornar() {
        HealthClassificationEntity entity = new HealthClassificationEntity();
        entity.setId(id1);
        entity.setDescription("Intolerante a la Lactosa");

        when(springDataRepository.findById(id1)).thenReturn(Optional.of(entity));

        Optional<HealthClassification> resultado = repositoryImpl.findById(id1);

        assertTrue(resultado.isPresent());
        assertEquals(id1, resultado.get().getId());
        assertEquals("Intolerante a la Lactosa", resultado.get().getDescription());

        verify(springDataRepository).findById(id1);
    }

    @Test
    void findById_CuandoNoExiste_DeberiaRetornarVacio() {
        when(springDataRepository.findById(id1)).thenReturn(Optional.empty());

        Optional<HealthClassification> resultado = repositoryImpl.findById(id1);

        assertTrue(resultado.isEmpty());
        verify(springDataRepository).findById(id1);
    }

    @Test
    void findAll_DeberiaMapearTodasLasEntidadesADominio() {
        HealthClassificationEntity entity1 = new HealthClassificationEntity();
        entity1.setId(UUID.randomUUID());
        entity1.setDescription("Sin TACC");

        when(springDataRepository.findAll()).thenReturn(List.of(entity1));

        List<HealthClassification> resultado = repositoryImpl.findAll();

        assertEquals(1, resultado.size());
        assertEquals("Sin TACC", resultado.get(0).getDescription());
        verify(springDataRepository).findAll();
    }

    @Test
    void findAll_CuandoNoHayDatos_DeberiaRetornarListaVacia() {
        when(springDataRepository.findAll()).thenReturn(List.of());

        List<HealthClassification> resultado = repositoryImpl.findAll();

        assertNotNull(resultado, "El repositorio nunca debería retornar null");
        assertTrue(resultado.isEmpty(), "La lista debería estar vacía");

        verify(springDataRepository).findAll();
    }
}