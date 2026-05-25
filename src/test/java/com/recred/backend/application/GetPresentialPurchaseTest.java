package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPresentialPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetPresentialPurchase useCase;

    @Test
    void execute_WhenPurchaseExists_ShouldReturnPurchase() {
        UUID purchaseId = UUID.randomUUID();
        Purchase expectedPurchase = new Purchase();
        expectedPurchase.setId(purchaseId);

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(expectedPurchase));

        Optional<Purchase> result = useCase.execute(purchaseId);

        assertTrue(result.isPresent());
        assertEquals(expectedPurchase, result.get());
        verify(purchaseRepository).findById(purchaseId);
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ShouldReturnEmpty() {
        UUID purchaseId = UUID.randomUUID();

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        Optional<Purchase> result = useCase.execute(purchaseId);

        assertTrue(result.isEmpty());
        verify(purchaseRepository).findById(purchaseId);
    }
}
