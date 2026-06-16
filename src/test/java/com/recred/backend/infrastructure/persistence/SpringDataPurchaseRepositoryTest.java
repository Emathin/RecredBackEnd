package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolAssistantPurchase;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringDataPurchaseRepositoryTest {

    @Test
    void findRecentByStudentIdWithLimitDelegatesToPageableQuery() {
        SpringDataPurchaseRepository repository =
                mock(SpringDataPurchaseRepository.class, CALLS_REAL_METHODS);
        UUID studentId = UUID.randomUUID();
        List<SchoolAssistantPurchase> purchases = List.of();
        when(repository.findRecentByStudentId(eq(studentId), any(Pageable.class)))
                .thenReturn(purchases);

        List<SchoolAssistantPurchase> result = repository.findRecentByStudentId(studentId, 5);

        assertSame(purchases, result);
        verify(repository).findRecentByStudentId(
                eq(studentId),
                argThat(pageable -> pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 5)
        );
    }
}
