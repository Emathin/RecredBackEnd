package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.List;

public class GetTimeRestrictionsByStudent {

    private final RestriccionHorariaRepository restrictionRepository;

    public GetTimeRestrictionsByStudent(RestriccionHorariaRepository restrictionRepository) {
        this.restrictionRepository = restrictionRepository;
    }

    public List<GetTimeRestrictionsByStudentResponse> execute(
            GetTimeRestrictionsByStudentRequest restrictionsRequest) {
        List<RestriccionHoraria> restrictions = findRestrictionsForStudent(restrictionsRequest);
        return responsesFrom(restrictions);
    }

    private List<RestriccionHoraria> findRestrictionsForStudent(
            GetTimeRestrictionsByStudentRequest restrictionsRequest) {
        return restrictionRepository.findAllByAlumnoEntityId(restrictionsRequest.studentId());
    }

    private List<GetTimeRestrictionsByStudentResponse> responsesFrom(
            List<RestriccionHoraria> restrictions) {
        return restrictions.stream()
                .map(this::responseFrom)
                .toList();
    }

    private GetTimeRestrictionsByStudentResponse responseFrom(
            RestriccionHoraria restriction) {
        return new GetTimeRestrictionsByStudentResponse(
                restriction.getId(),
                restriction.getAlumnoId(),
                timeSlotResponseFrom(restriction.getFranjaHoraria()),
                categoryResponseFrom(restriction.getCategoria()),
                healthClassificationResponseFrom(restriction.getClasificacionSalud()),
                restriction.isActiva(),
                restriction.getCreadoPorUsuarioId()
        );
    }

    private GetTimeRestrictionsByStudentResponse.FranjaHorariaResponse timeSlotResponseFrom(
            FranjaHoraria timeSlot) {
        if (timeSlot == null) {
            return null;
        }
        return new GetTimeRestrictionsByStudentResponse.FranjaHorariaResponse(
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

    private GetTimeRestrictionsByStudentResponse.CategoriaProductoResponse categoryResponseFrom(
            CategoriaProducto category) {
        if (category == null) {
            return null;
        }
        return new GetTimeRestrictionsByStudentResponse.CategoriaProductoResponse(
                category.getId(),
                category.getDescripcion(),
                category.getActivo()
        );
    }

    private GetTimeRestrictionsByStudentResponse.ClasificacionSaludResponse
            healthClassificationResponseFrom(
            ClasificacionSalud healthClassification) {
        if (healthClassification == null) {
            return null;
        }
        return new GetTimeRestrictionsByStudentResponse.ClasificacionSaludResponse(
                healthClassification.getId(),
                healthClassification.getDescripcion(),
                healthClassification.getActivo()
        );
    }
}
