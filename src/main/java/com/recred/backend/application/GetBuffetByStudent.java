package com.recred.backend.application;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;

import java.util.Optional;
import java.util.UUID;

public class GetBuffetByStudent {

    private final BuffetRepository buffetRepository;
    private final AlumnoRepository alumnoRepository;

    public GetBuffetByStudent(
            BuffetRepository buffetRepository, AlumnoRepository alumnoRepository) {
        this.buffetRepository = buffetRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public boolean existsById(UUID id) {
        return alumnoRepository.existsById(id);
    }

    private void analizarExcepcionAlumnoNoEncontrado(UUID alumnoId) {
        if (!existsById(alumnoId)) {
            throw new AlumnoNoEncontradoException(alumnoId);
        }
    }

    public Optional<GetBuffetByStudentResponse> execute(GetBuffetByStudentRequest request) {
        analizarExcepcionAlumnoNoEncontrado(request.alumnoId());
        return buffetRepository.findByAlumnoId(request.alumnoId())
                .map(this::mapToResponse);
    }

    private GetBuffetByStudentResponse mapToResponse(Buffet buffet) {
        return new GetBuffetByStudentResponse(
                buffet.getId(),
                buffet.getNombre(),
                buffet.getColegio() != null ? buffet.getColegio().getId() : null,
                buffet.getVendedor() != null ? buffet.getVendedor().getId() : null,
                buffet.getFechaVencimientoHabilitacion(),
                buffet.getActivo(),
                buffet.getCreadoEn(),
                buffet.getActualizadoEn()
        );
    }
}
