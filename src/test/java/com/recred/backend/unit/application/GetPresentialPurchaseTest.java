package com.recred.backend.unit.application;

import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchaseRequest;
import com.recred.backend.application.GetPresentialPurchaseResponse;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPresentialPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetPresentialPurchase useCase;

    private UUID purchaseId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStatus(EstadoCompra.PENDIENTE);
    }

    @Test
    void execute_WhenPurchaseExists_ShouldReturnResponse() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        GetPresentialPurchaseResponse response = useCase.execute(new GetPresentialPurchaseRequest(purchaseId));

        assertNotNull(response);
        assertEquals(purchaseId, response.id());
        assertEquals("PENDIENTE", response.status());
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        assertThrows(PurchaseNotFoundException.class,
                () -> useCase.execute(new GetPresentialPurchaseRequest(purchaseId)));
    }
}
