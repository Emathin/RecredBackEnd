package com.recred.backend.application;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.TypeClassification;
import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;

public class CreateTimeRestriction {

    private final TimeRestrictionRepository repository;

    public CreateTimeRestriction(TimeRestrictionRepository repository) {
        this.repository = repository;
    }

    public TimeRestriction execute(TimeRestrictionCommand command) {
        TimeBadge timeSlot = new TimeBadge();
        timeSlot.setId(command.timeSlotId());

        ProductCategory category = null;
        if (command.categoryId() != null) {
            category = new ProductCategory();
            category.setId(command.categoryId());
        }

        TypeClassification classification = null;
        if (command.classificationId() != null) {
            classification = new TypeClassification();
            classification.setId(command.classificationId());
        }

        TimeRestriction restriction = new TimeRestriction(
                null,
                command.studentId(),
                timeSlot,
                category,
                classification
        );

        return repository.save(restriction);
    }
}