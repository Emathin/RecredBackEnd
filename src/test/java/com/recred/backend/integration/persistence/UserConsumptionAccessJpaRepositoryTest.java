package com.recred.backend.integration.persistence;

import java.util.UUID;

import com.recred.backend.infrastructure.persistence.UserConsumptionAccessJpaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserConsumptionAccessJpaRepositoryTest {

    @Test
    void tutorCanAccessStudentWhenAssociationExists() {
        UserConsumptionAccessJpaRepository repository = mock(
                UserConsumptionAccessJpaRepository.class,
                CALLS_REAL_METHODS
        );
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(repository.countTutorStudents(userId, studentId)).thenReturn(1L);

        assertTrue(repository.tutorCanAccessStudent(userId, studentId));
    }

    @Test
    void tutorCannotAccessStudentWhenAssociationDoesNotExist() {
        UserConsumptionAccessJpaRepository repository = mock(
                UserConsumptionAccessJpaRepository.class,
                CALLS_REAL_METHODS
        );
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(repository.countTutorStudents(userId, studentId)).thenReturn(0L);

        assertFalse(repository.tutorCanAccessStudent(userId, studentId));
    }
}
