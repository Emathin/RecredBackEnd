package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentHistory;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentHistoryTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetStudentHistory getStudentHistory;

    @Test
    void executeWhenStudentHasPurchasesShouldReturnPurchaseList() {
        UUID studentId = UUID.randomUUID();
        Purchase firstPurchase = new Purchase();
        Purchase secondPurchase = new Purchase();
        List<Purchase> mockPurchases = List.of(firstPurchase, secondPurchase);

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(mockPurchases);

        List<Purchase> result = getStudentHistory.execute(studentId);

        assertEquals(2, result.size());
        verify(purchaseRepository, times(1)).findHistorialByAlumno(studentId);
    }
}
