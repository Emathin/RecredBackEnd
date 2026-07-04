package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.SchoolCreditEntity;
import com.recred.backend.infrastructure.persistence.SchoolCreditRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataSchoolCreditRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolCreditRepositoryImplTest {

    @Mock
    private SpringDataSchoolCreditRepository repository;

    @Mock
    private SpringDataAlumnoRepository alumnoRepository;

    @Test
    void findByStudentIdAndStatusShouldMapEntity() {
        SchoolCreditRepositoryImpl adapter =
                new SchoolCreditRepositoryImpl(repository, alumnoRepository);
        UUID studentId = UUID.randomUUID();
        SchoolCreditEntity entity = entity(UUID.randomUUID(), studentId);
        when(repository.findByAlumnoIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        Optional<SchoolCredit> result = adapter.findByStudentIdAndStatus(
                studentId,
                CreditStatus.ACTIVE
        );

        assertEquals(studentId, result.orElseThrow().getStudentId());
        assertEquals(CreditStatus.ACTIVE, result.orElseThrow().getStatus());
    }

    @Test
    void saveShouldAttachStudentWhenStudentIdIsPresent() {
        SchoolCreditRepositoryImpl adapter =
                new SchoolCreditRepositoryImpl(repository, alumnoRepository);
        UUID studentId = UUID.randomUUID();
        SchoolCredit credit = credit(studentId);
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(studentId);
        ArgumentCaptor<SchoolCreditEntity> captor =
                ArgumentCaptor.forClass(SchoolCreditEntity.class);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(repository.save(any(SchoolCreditEntity.class))).thenAnswer(invocation -> {
            SchoolCreditEntity entity = invocation.getArgument(0);
            entity.setId(credit.getId());
            return entity;
        });

        SchoolCredit saved = adapter.save(credit);

        verify(repository).save(captor.capture());
        assertEquals(studentId, captor.getValue().getAlumno().getId());
        assertEquals(credit.getAmount(), captor.getValue().getAmount());
        assertEquals(studentId, saved.getStudentId());
    }

    @Test
    void saveShouldAllowCreditWithoutStudentId() {
        SchoolCreditRepositoryImpl adapter =
                new SchoolCreditRepositoryImpl(repository, alumnoRepository);
        SchoolCredit credit = credit(null);
        when(repository.save(any(SchoolCreditEntity.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        SchoolCredit saved = adapter.save(credit);

        assertNull(saved.getStudentId());
    }

    @Test
    void saveShouldFailWhenStudentDoesNotExist() {
        SchoolCreditRepositoryImpl adapter =
                new SchoolCreditRepositoryImpl(repository, alumnoRepository);
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.save(credit(studentId))
        );

        assertEquals("Student not found", exception.getMessage());
    }

    private SchoolCredit credit(UUID studentId) {
        return new SchoolCredit(
                UUID.randomUUID(),
                studentId,
                new BigDecimal("500.00"),
                CreditStatus.ACTIVE,
                1,
                LocalDateTime.of(2026, 7, 4, 12, 0)
        );
    }

    private SchoolCreditEntity entity(UUID id, UUID studentId) {
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(studentId);
        SchoolCreditEntity entity = new SchoolCreditEntity();
        entity.setId(id);
        entity.setAlumno(alumno);
        entity.setAmount(new BigDecimal("500.00"));
        entity.setStatus(CreditStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.of(2026, 7, 4, 12, 0));
        return entity;
    }
}
