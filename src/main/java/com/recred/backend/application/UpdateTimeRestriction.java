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
        UUID restrictionId = request.restrictionId();
        RestriccionHoraria existingRestriction = repository.findById(restrictionId)
                .orElseThrow(() -> new RuntimeException("Restricción no encontrada"));

        Alumno alumno = alumnoRepository.findById(existingRestriction.getAlumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        if (request.classificationId() != null) {
            boolean saludGlobalActiva = alumno.getRestriccionesSalud().stream()
                    .anyMatch(r -> r.getId().equals(request.classificationId()));
            if (saludGlobalActiva) {
                throw new IllegalArgumentException(
                        "El alumno ya posee esta restricción de salud asignada globalmente.");
            }
        }

        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(request.timeSlotId());
        existingRestriction.setFranjaHoraria(timeSlot);

        if (request.categoryId() != null) {
            CategoriaProducto category = new CategoriaProducto();
            category.setId(request.categoryId());
            existingRestriction.setCategoria(category);
        } else {
            existingRestriction.setCategoria(null);
        }

        if (request.classificationId() != null) {
            ClasificacionSalud classification = new ClasificacionSalud();
            classification.setId(request.classificationId());
            existingRestriction.setClasificacionSalud(classification);
        } else {
            existingRestriction.setClasificacionSalud(null);
        }

        RestriccionHoraria saved = repository.save(existingRestriction);
        return mapToResponse(saved);
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