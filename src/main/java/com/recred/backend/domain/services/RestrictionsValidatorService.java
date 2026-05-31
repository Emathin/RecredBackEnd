package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.HealthRestrictionException;
import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;

import java.util.stream.Collectors;
import java.util.Set;

public class RestrictionsValidatorService {

    public void validatePurchase(Student student, Set<Product> products) {
        for (Product product : products) {
            Set<HealthClassification> conflicts = student.getHealthRestrictions().stream()
                    .filter(restriction -> product.getHealthClassifications().contains(restriction))
                    .collect(Collectors.toSet());

            if (!conflicts.isEmpty()) {
                String restrictionNames = conflicts.stream()
                        .map(HealthClassification::getDescription)
                        .collect(Collectors.joining(", "));

                throw new HealthRestrictionException(
                        "El producto " + product.getName() + " no es apto para el " +
                                "alumno debido a: " + restrictionNames
                );
            }
        }
    }
}