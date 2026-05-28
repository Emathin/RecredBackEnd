package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

public class CreateTimeRestriction {

    private final RestriccionHorariaRepository repository;

    public CreateTimeRestriction(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public RestriccionHoraria execute(TimeRestrictionCommand command) {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(command.timeSlotId());

        CategoriaProducto category = null;
        if (command.categoryId() != null) {
            category = new CategoriaProducto();
            category.setId(command.categoryId());
        }

        ClasificacionTipo classification = null;
        if (command.classificationId() != null) {
            classification = new ClasificacionTipo();
            classification.setId(command.classificationId());
        }

        RestriccionHoraria restriction = new RestriccionHoraria(
                null,
                command.studentId(),
                timeSlot,
                category,
                classification
        );

        return repository.save(restriction);
    }
}