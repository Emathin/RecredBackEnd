package com.recred.backend.application;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.TypeClassification;
import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;

import java.util.UUID;

public class UpdateTimeRestriction {

    private final TimeRestrictionRepository repository;

    public UpdateTimeRestriction(TimeRestrictionRepository repository) {
        this.repository = repository;
    }

    public TimeRestriction execute(UUID restrictionId, TimeRestrictionCommand command) {
        TimeRestriction existingRestriction = repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));

        TimeBadge timeSlot = new TimeBadge();
        timeSlot.setId(command.timeSlotId());
        existingRestriction.setTimeBadge(timeSlot);

        if (command.categoryId() != null) {
            ProductCategory category = new ProductCategory();
            category.setId(command.categoryId());
            existingRestriction.setCategory(category);
        } else {
            existingRestriction.setCategory(null);
        }

        if (command.classificationId() != null) {
            TypeClassification classification = new TypeClassification();
            classification.setId(command.classificationId());
            existingRestriction.setClassification(classification);
        } else {
            existingRestriction.setClassification(null);
        }

        return repository.save(existingRestriction);
    }
}