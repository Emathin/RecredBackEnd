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

    public UpdateTimeRestrictionResponse execute(UpdateTimeRestrictionRequest request) {
        RestriccionHoraria existingRestriction = this.findRestrictionById(request.restrictionId());
        Alumno alumno = this.findAlumnoById(existingRestriction.getAlumnoId());

        this.validateClassificationConstraints(alumno, request.classificationId());
        this.updateRestrictionFields(existingRestriction, request);

        RestriccionHoraria saved = this.saveRestriction(existingRestriction);
        return mapToResponse(saved);
    }

    private RestriccionHoraria findRestrictionById(UUID restrictionId) {
        return repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));
    }

    private Alumno findAlumnoById(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
    }

    private void validateClassificationConstraints(Alumno alumno, UUID classificationId) {
        if (classificationId != null) {
            boolean saludGlobalActiva = alumno.getRestriccionesSalud().stream()
                    .anyMatch(r -> r.getId().equals(classificationId));
            if (saludGlobalActiva) {
                throw new IllegalArgumentException(
                        "El alumno ya posee esta restricción de salud asignada globalmente.");
            }
        }
    }

    private void updateRestrictionFields(
            RestriccionHoraria restriction, UpdateTimeRestrictionRequest request) {
        this.updateTimeSlot(restriction, request.timeSlotId());
        this.updateCategory(restriction, request.categoryId());
        this.updateClassification(restriction, request.classificationId());
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

    private UpdateTimeRestrictionResponse mapToResponse(RestriccionHoraria r) {
        return new UpdateTimeRestrictionResponse(
                r.getId(),
                r.getAlumnoId(),
                mapFranja(r.getFranjaHoraria()),
                mapCategoria(r.getCategoria()),
                mapClasificacion(r.getClasificacionSalud()),
                r.isActiva(),
                r.getCreadoPorUsuarioId()
        );
    }

    private UpdateTimeRestrictionResponse.FranjaHorariaResponse mapFranja(FranjaHoraria fh) {
        if (fh == null) {
            return null;
        }
        return new UpdateTimeRestrictionResponse.FranjaHorariaResponse(
                fh.getId(),
                fh.getColegioId(),
                fh.getDescripcion(),
                fh.getHoraInicio(),
                fh.getHoraFin(),
                fh.isActivo(),
                fh.getCupoMaximo(),
                fh.getMinutosCorte()
        );
    }

    private UpdateTimeRestrictionResponse.CategoriaProductoResponse mapCategoria(
            CategoriaProducto cp) {
        if (cp == null) {
            return null;
        }
        return new UpdateTimeRestrictionResponse.CategoriaProductoResponse(
                cp.getId(),
                cp.getDescripcion(),
                cp.getActivo()
        );
    }

    private UpdateTimeRestrictionResponse.ClasificacionSaludResponse mapClasificacion(
            ClasificacionSalud cs) {
        if (cs == null) {
            return null;
        }
        return new UpdateTimeRestrictionResponse.ClasificacionSaludResponse(
                cs.getId(),
                cs.getDescripcion(),
                cs.getActivo()
        );
    }
}