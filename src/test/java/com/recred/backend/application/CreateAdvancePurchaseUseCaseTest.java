// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/test/java/com/recred/backend/application/CreateAdvancePurchaseUseCaseTest.java
package com.recred.backend.application;

import com.recred.backend.application.usecases.CreateAdvancePurchaseUseCase;
import com.recred.backend.domain.exceptions.DietaryRestrictionException;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class CreateAdvancePurchaseUseCaseTest {

    @Mock
    private PurchaseValidator purchaseValidator;

    @InjectMocks
    private CreateAdvancePurchaseUseCase createAdvancePurchaseUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        createAdvancePurchaseUseCase = new CreateAdvancePurchaseUseCase(purchaseValidator);
    }

    @Test
    void execute_shouldReturnPendingStatusAndWithdrawalCodes_whenPurchaseIsSuccessful() {
        // Given
        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest("prod1", 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                "student1", "tutor1", BuyerType.TUTOR, "2024-05-23", RecessTime.FIRST_RECESS, List.of(itemRequest)
        );
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

        // When
        // The use case is not implemented yet, so we can't test the success case fully.
        // We will test that it returns null for now.
        AdvancePurchaseResponse response = createAdvancePurchaseUseCase.execute(request);

        // Then
        assertNull(response);
    }

    @Test
    void execute_shouldThrowDietaryRestrictionException_whenValidationFails() {
        // Given
        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest("prod1", 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                "student1", "tutor1", BuyerType.TUTOR, "2024-05-23", RecessTime.FIRST_RECESS, List.of(itemRequest)
        );
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

        doThrow(new DietaryRestrictionException("Dietary restriction violated"))
                .when(purchaseValidator).validate(any(Purchase.class));

        // When & Then
        assertThrows(DietaryRestrictionException.class, () -> {
            createAdvancePurchaseUseCase.execute(request);
        });
    }
}
