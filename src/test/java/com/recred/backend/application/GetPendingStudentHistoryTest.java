package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPendingStudentHistoryTest {

    @Test
    void executeShouldReturnPendingPurchasesFromRepository() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        GetPendingStudentHistory useCase =
                new GetPendingStudentHistory(purchaseRepository);
        UUID studentId = UUID.randomUUID();
        Purchase purchase = new Purchase();
        when(purchaseRepository.findPendientesByAlumno(studentId))
                .thenReturn(List.of(purchase));

        List<Purchase> result = useCase.execute(studentId);

        assertEquals(List.of(purchase), result);
    }
}
