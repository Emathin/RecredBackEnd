package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolAssistantPayment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchoolAssistantPaymentJpaRepositoryTest {

    @Test
    void findRecentByStudentId_ShouldUsePageRequestLimit() {
        SchoolAssistantPaymentJpaRepository repository = mock(
                SchoolAssistantPaymentJpaRepository.class,
                CALLS_REAL_METHODS
        );
        UUID studentId = UUID.randomUUID();
        List<SchoolAssistantPayment> expected = List.of();
        doReturn(expected).when(repository).findRecentByStudentId(eq(studentId), any(Pageable.class));

        List<SchoolAssistantPayment> result = repository.findRecentByStudentId(studentId, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findRecentByStudentId(eq(studentId), pageableCaptor.capture());
        assertEquals(expected, result);
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void findByStudentIdAndDateRange_ShouldUsePageRequestLimit() {
        SchoolAssistantPaymentJpaRepository repository = mock(
                SchoolAssistantPaymentJpaRepository.class,
                CALLS_REAL_METHODS
        );
        UUID studentId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 15, 23, 59);
        List<SchoolAssistantPayment> expected = List.of();
        doReturn(expected).when(repository).findByStudentIdAndDateRange(
                eq(studentId),
                eq(start),
                eq(end),
                any(Pageable.class)
        );

        List<SchoolAssistantPayment> result =
                repository.findByStudentIdAndDateRange(studentId, start, end, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByStudentIdAndDateRange(
                eq(studentId),
                eq(start),
                eq(end),
                pageableCaptor.capture()
        );
        assertEquals(expected, result);
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }
}
