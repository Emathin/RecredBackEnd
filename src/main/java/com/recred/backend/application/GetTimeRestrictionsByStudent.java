package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;

import java.util.List;

public class GetTimeRestrictionsByStudent {

    private final RestriccionHorariaRepository repository;

    public GetTimeRestrictionsByStudent(RestriccionHorariaRepository repository) {
        this.repository = repository;
    }

    public List<GetTimeRestrictionsByStudentResponse> execute(
            GetTimeRestrictionsByStudentRequest request) {
        List<RestriccionHoraria> restrictions = repository
                .findAllByAlumnoEntityId(request.studentId());
        return restrictions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GetTimeRestrictionsByStudentResponse mapToResponse(RestriccionHoraria r) {
        return new GetTimeRestrictionsByStudentResponse(
                r.getId(),
                r.getAlumnoId(),
                mapFranja(r.getFranjaHoraria()),
                mapCategoria(r.getCategoria()),
                mapClasificacion(r.getClasificacionSalud()),
                r.isActiva(),
                r.getCreadoPorUsuarioId()
        );
    }

    private GetTimeRestrictionsByStudentResponse.FranjaHorariaResponse mapFranja(
            FranjaHoraria fh) {
        if (fh == null) {
            return null;
        }
        return new GetTimeRestrictionsByStudentResponse.FranjaHorariaResponse(
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

    private GetTimeRestrictionsByStudentResponse.CategoriaProductoResponse mapCategoria(
            CategoriaProducto cp) {
        if (cp == null) {
            return null;
        }
        return new GetTimeRestrictionsByStudentResponse.CategoriaProductoResponse(
                cp.getId(),
                cp.getDescripcion(),
                cp.getActivo()
        );
    }

    private GetTimeRestrictionsByStudentResponse.ClasificacionSaludResponse mapClasificacion(
            ClasificacionSalud cs) {
        if (cs == null) {
            return null;
        }
        return new GetTimeRestrictionsByStudentResponse.ClasificacionSaludResponse(
                cs.getId(),
                cs.getDescripcion(),
                cs.getActivo()
        );
    }
}