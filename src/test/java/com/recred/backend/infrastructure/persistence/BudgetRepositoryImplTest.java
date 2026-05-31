package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.infrastructure.entities.StudentBudgetEntity;
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
class BudgetRepositoryImplTest {

    @Mock
    private SpringDataPresupuestoRepository springDataRepository;

    @InjectMocks
    private BudgetRepositoryImpl repositoryImpl;

    private UUID studentId;
    private StudentBudgetEntity entityMock;
    private StudentBudget domainMock;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        entityMock = new StudentBudgetEntity();
        entityMock.setId(UUID.randomUUID());

        domainMock = new StudentBudget();
        domainMock.setId(entityMock.getId());
    }

    @Test
    void findByActiveStudentID_SiExiste_RetornaDominioMapeado() {

        when(springDataRepository.findByAlumnoEntityIdAndActivoTrue(studentId)).thenReturn(Optional.of(entityMock));

        Optional<StudentBudget> result = repositoryImpl.findByActiveStudentID(studentId);

        assertTrue(result.isPresent());

        assertEquals(entityMock.getId(), result.get().getId());
        verify(springDataRepository).findByAlumnoEntityIdAndActivoTrue(studentId);
    }

    @Test
    void findByActiveStudentID_SiNoExiste_RetornaEmpty() {
        when(springDataRepository.findByAlumnoEntityIdAndActivoTrue(studentId)).thenReturn(Optional.empty());

        Optional<StudentBudget> result = repositoryImpl.findByActiveStudentID(studentId);

        assertTrue(result.isEmpty());
        verify(springDataRepository).findByAlumnoEntityIdAndActivoTrue(studentId);
    }

    @Test
    void save_ConvierteADominio_Guarda_YRetornaDominio() {

        when(springDataRepository.save(any(StudentBudgetEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        StudentBudget resultado = repositoryImpl.save(domainMock);

        assertNotNull(resultado);
        assertEquals(domainMock.getId(), resultado.getId());

        verify(springDataRepository).save(any(StudentBudgetEntity.class));
    }
}