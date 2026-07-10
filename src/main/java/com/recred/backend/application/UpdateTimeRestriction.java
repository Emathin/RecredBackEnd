package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.UUID;

public class UpdateTimeRestriction {

    private final RestriccionHorariaRepository repository;
    private final AlumnoRepository studentRepository;

    public UpdateTimeRestriction(
            RestriccionHorariaRepository repository,
            AlumnoRepository studentRepository) {
        this.repository = repository;
        this.studentRepository = studentRepository;
    }

    public UpdateTimeRestrictionResponse execute(UpdateTimeRestrictionRequest request) {
        RestriccionHoraria existingRestriction = findRestriction(request.restrictionId());
        updateRestriction(existingRestriction, request);

        return responseFrom(saveRestriction(existingRestriction));
    }

    private RestriccionHoraria findRestriction(UUID restrictionId) {
        return repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));
    }

    private Alumno findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
    }

    private void ensureClassificationIsNotGloballyAssigned(
            Alumno student,
            UUID classificationId) {
        if (classificationId == null) {
            return;
        }

        boolean activeGlobalHealthRestriction = student.getRestriccionesSalud().stream()
                .anyMatch(restriction -> restriction.getId().equals(classificationId));
        if (activeGlobalHealthRestriction) {
            throw new IllegalArgumentException(
                    "El alumno ya posee esta restricción de salud asignada globalmente.");
        }
    }

    private void updateRestriction(
            RestriccionHoraria restriction,
            UpdateTimeRestrictionRequest request) {
        Alumno student = findStudent(restriction.getAlumnoId());
        ensureClassificationIsNotGloballyAssigned(student, request.classificationId());
        updateRestrictionFields(restriction, request);
    }

    private void updateRestrictionFields(
            RestriccionHoraria restriction,
            UpdateTimeRestrictionRequest request) {
        updateTimeSlot(restriction, request.timeSlotId());
        updateCategory(restriction, request.categoryId());
        updateClassification(restriction, request.classificationId());
    }

    private void updateTimeSlot(RestriccionHoraria restriction, UUID timeSlotId) {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(timeSlotId);
        restriction.setFranjaHoraria(timeSlot);
    }

    private void updateCategory(RestriccionHoraria restriction, UUID categoryId) {
        if (categoryId != null) {
            CategoriaProducto category = new CategoriaProducto();
            category.setId(categoryId);
            restriction.setCategoria(category);
        } else {
            restriction.setCategoria(null);
        }
    }

    private void updateClassification(RestriccionHoraria restriction, UUID classificationId) {
        if (classificationId != null) {
            ClasificacionSalud classification = new ClasificacionSalud();
            classification.setId(classificationId);
            restriction.setClasificacionSalud(classification);
        } else {
            restriction.setClasificacionSalud(null);
        }
    }

    private RestriccionHoraria saveRestriction(RestriccionHoraria restriction) {
        return repository.save(restriction);
    }

    private UpdateTimeRestrictionResponse responseFrom(RestriccionHoraria restriction) {
        return new UpdateTimeRestrictionResponse(
                restriction.getId(),
                restriction.getAlumnoId(),
                timeSlotResponseFrom(restriction.getFranjaHoraria()),
                categoryResponseFrom(restriction.getCategoria()),
                classificationResponseFrom(restriction.getClasificacionSalud()),
                restriction.isActiva(),
                restriction.getCreadoPorUsuarioId()
        );
    }

    private UpdateTimeRestrictionResponse.FranjaHorariaResponse timeSlotResponseFrom(
            FranjaHoraria timeSlot) {
        if (timeSlot == null) {
            return null;
        }
        return new UpdateTimeRestrictionResponse.FranjaHorariaResponse(
                timeSlot.getId(),
                timeSlot.getColegioId(),
                timeSlot.getDescripcion(),
                timeSlot.getHoraInicio(),
                timeSlot.getHoraFin(),
                timeSlot.isActivo(),
                timeSlot.getCupoMaximo(),
                timeSlot.getMinutosCorte()
        );
    }

    private UpdateTimeRestrictionResponse.CategoriaProductoResponse categoryResponseFrom(
            CategoriaProducto category) {
        if (category == null) {
            return null;
        }
        return new UpdateTimeRestrictionResponse.CategoriaProductoResponse(
                category.getId(),
                category.getDescripcion(),
                category.getActivo()
        );
    }

    private UpdateTimeRestrictionResponse.ClasificacionSaludResponse classificationResponseFrom(
            ClasificacionSalud classification) {
        if (classification == null) {
            return null;
        }
        return new UpdateTimeRestrictionResponse.ClasificacionSaludResponse(
                classification.getId(),
                classification.getDescripcion(),
                classification.getActivo()
        );
    }
}
