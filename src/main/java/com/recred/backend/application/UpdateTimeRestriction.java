package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.UUID;

public class UpdateTimeRestriction {

    private final RestriccionHorariaRepository repository;
    private final AlumnoRepository alumnoRepository;

    public UpdateTimeRestriction(
            RestriccionHorariaRepository repository, 
            AlumnoRepository alumnoRepository) {
        this.repository = repository;
        this.alumnoRepository = alumnoRepository;
    }

    public RestriccionHoraria execute(UUID restrictionId, TimeRestrictionCommand command) {
        RestriccionHoraria existingRestriction = repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));

        Alumno alumno = alumnoRepository.findById(existingRestriction.getAlumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        if (command.classificationId() != null) {
            boolean saludGlobalActiva = alumno.getRestriccionesSalud().stream()
                    .anyMatch(r -> r.getId().equals(command.classificationId()));
            if (saludGlobalActiva) {
                throw new IllegalArgumentException(
                        "El alumno ya posee esta restricción de salud asignada globalmente.");
            }
        }

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
            ClasificacionSalud classification = new ClasificacionSalud();
            classification.setId(command.classificationId());
            existingRestriction.setClasificacionSalud(classification);
        } else {
            existingRestriction.setClasificacionSalud(null);
        }

        return repository.save(existingRestriction);
    }
}