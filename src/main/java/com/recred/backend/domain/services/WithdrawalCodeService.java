// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/main/java/com/recred/backend/domain/services/WithdrawalCodeService.java
package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;

public interface WithdrawalCodeService {
    String generateCode(Purchase purchase);
}
