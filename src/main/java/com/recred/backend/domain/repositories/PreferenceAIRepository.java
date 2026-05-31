package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Product;

import java.util.List;
import java.util.Optional;

public interface PreferenceAIRepository {

    Optional<DetectedPreference> analizarPreferencias(Student student,
                                                      List<PurchaseSummary> history,
                                                      List<Product> availableStock);

    record PurchaseSummary(String productName, String category, java.time.LocalDateTime date) {}
    
    record DetectedPreference(String tittle, String message, String productoId, String razonIA) {}
}
