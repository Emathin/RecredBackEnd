package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePurchaseStatusTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private UpdatePurchaseStatus useCase;

    private UUID purchaseId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();

        Student student = new Student();
        student.setId(UUID.randomUUID());

        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setStatus(PurchaseState.PENDIENTE);
    }

    @Test
    void execute_WhenTransitionIsValid_ShouldSaveAndReturnStatus() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.save(purchase)).thenReturn(purchase);

        PurchaseStatus result = useCase.execute(purchaseId, PurchaseState.EN_PREPARACION);

        assertEquals(PurchaseState.EN_PREPARACION, purchase.getStatus());
        assertEquals(PurchaseState.EN_PREPARACION, result.status());
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void execute_WhenTransitionIsInvalid_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> useCase.execute(purchaseId, PurchaseState.LISTO));

        verify(purchaseRepository, never()).save(purchase);
    }

    @Test
    void execute_WhenRequestedStatusIsCancelled_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> useCase.execute(purchaseId, PurchaseState.CANCELADO));

        verify(purchaseRepository, never()).save(purchase);
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        assertThrows(PurchaseNotFoundException.class,
                () -> useCase.execute(purchaseId, PurchaseState.EN_PREPARACION));
    }
}
