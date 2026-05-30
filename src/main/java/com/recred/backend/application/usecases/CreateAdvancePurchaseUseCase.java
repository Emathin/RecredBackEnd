// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/main/java/com/recred/backend/application/usecases/CreateAdvancePurchaseUseCase.java
package com.recred.backend.application.usecases;

import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;

public class CreateAdvancePurchaseUseCase {

    private final PurchaseValidator purchaseValidator;

    public CreateAdvancePurchaseUseCase(PurchaseValidator purchaseValidator) {
        this.purchaseValidator = purchaseValidator;
    }

    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request) {
        // This is a skeleton and will be implemented in the next phase.
        return null;
    }
}
