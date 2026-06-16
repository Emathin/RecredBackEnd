package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import com.recred.backend.infrastructure.entities.ReglaPresupuestoCategoriaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    void save_WhenBudgetHasReferencesAndRules_ShouldMapThemToEntity() {
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        domainMock.setAlumnoId(studentId);
        domainMock.setCreadoPorUsuarioId(creatorId);
        domainMock.setMontoLimiteGeneral(new BigDecimal("10000.00"));
        domainMock.setPeriodo(PeriodoPresupuesto.MENSUAL);
        domainMock.setFechaInicio(LocalDate.of(2026, 6, 1));
        domainMock.setReglasCategoria(List.of(rule(categoryId)));
        when(springDataRepository.findById(domainMock.getId()))
                .thenReturn(Optional.empty());
        when(springDataRepository.save(any(PresupuestoAlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        repositoryImpl.save(domainMock);

        ArgumentCaptor<PresupuestoAlumnoEntity> captor =
                ArgumentCaptor.forClass(PresupuestoAlumnoEntity.class);
        verify(springDataRepository).save(captor.capture());
        PresupuestoAlumnoEntity entity = captor.getValue();
        assertEquals(studentId, entity.getAlumnoEntity().getId());
        assertEquals(creatorId, entity.getCreadoPorUsuarioEntity().getId());
        assertEquals(new BigDecimal("10000.00"), entity.getMontoLimite());
        assertEquals(1, entity.getReglasCategoria().size());
        assertEquals(categoryId, entity.getReglasCategoria().get(0).getCategoria().getId());
        assertEquals(entity, entity.getReglasCategoria().get(0).getPresupuesto());
    }

    @Test
    void save_WhenBudgetHasNoId_ShouldCreateEntity() {
        domainMock.setId(null);
        domainMock.setAlumnoId(studentId);
        when(springDataRepository.save(any(PresupuestoAlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PresupuestoAlumno result = repositoryImpl.save(domainMock);

        assertEquals(null, result.getId());
        verify(springDataRepository).save(any(PresupuestoAlumnoEntity.class));
    }

    @Test
    void save_WhenExistingRulesCollectionIsNull_ShouldSetRulesCollection() {
        entityMock.setReglasCategoria(null);
        domainMock.setAlumnoId(studentId);
        domainMock.setReglasCategoria(List.of(rule(UUID.randomUUID())));
        when(springDataRepository.findById(domainMock.getId()))
                .thenReturn(Optional.of(entityMock));
        when(springDataRepository.save(any(PresupuestoAlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        repositoryImpl.save(domainMock);

        assertEquals(1, entityMock.getReglasCategoria().size());
    }

    @Test
    void save_WhenExistingBudgetHasRules_ShouldClearAndFlushBeforeReplacing() {
        ReglaPresupuestoCategoriaEntity existingRule =
                new ReglaPresupuestoCategoriaEntity();
        entityMock.setReglasCategoria(new java.util.ArrayList<>(List.of(existingRule)));
        domainMock.setAlumnoId(studentId);
        domainMock.setReglasCategoria(List.of(rule(UUID.randomUUID())));
        when(springDataRepository.findById(domainMock.getId()))
                .thenReturn(Optional.of(entityMock));
        when(springDataRepository.saveAndFlush(entityMock)).thenReturn(entityMock);
        when(springDataRepository.save(any(PresupuestoAlumnoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        repositoryImpl.save(domainMock);

        verify(springDataRepository).saveAndFlush(entityMock);
        assertEquals(1, entityMock.getReglasCategoria().size());
    }

    @Test
    void findById_WhenBudgetExists_ShouldReturnDomain() {
        when(springDataRepository.findById(entityMock.getId()))
                .thenReturn(Optional.of(entityMock));

        Optional<PresupuestoAlumno> result = repositoryImpl.findById(entityMock.getId());

        assertTrue(result.isPresent());
        assertEquals(entityMock.getId(), result.get().getId());
    }

    private ReglaPresupuestoCategoria rule(UUID categoryId) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        return new ReglaPresupuestoCategoria(
                UUID.randomUUID(),
                category,
                new BigDecimal("30.00"),
                new BigDecimal("3000.00"),
                true
        );
    }
}
