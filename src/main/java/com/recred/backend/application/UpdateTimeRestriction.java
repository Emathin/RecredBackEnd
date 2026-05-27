package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.UUID;

public class UpdateTimeRestriction {

    private final RestriccionHorariaRepository repository;

    public UpdateTimeRestriction(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public RestriccionHoraria execute(UUID restrictionId, TimeRestrictionCommand command) {
        RestriccionHoraria existingRestriction = repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));

        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(command.timeSlotId());
        existingRestriction.setFranjaHoraria(timeSlot);

        if (command.categoryId() != null) {
            CategoriaProducto category = new CategoriaProducto();
            category.setId(command.categoryId());
            existingRestriction.setCategoria(category);
        } else {
            existingRestriction.setCategoria(null);
        }

        if (command.classificationId() != null) {
            ClasificacionTipo classification = new ClasificacionTipo();
            classification.setId(command.classificationId());
            existingRestriction.setClasificacion(classification);
        } else {
            existingRestriction.setClasificacion(null);
        }

        return repository.save(existingRestriction);
    }
}