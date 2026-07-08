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

    public Optional<GetBuffetByStudentResponse> execute(
            GetBuffetByStudentRequest buffetRequest) {
        ensureStudentExists(buffetRequest.alumnoId());
        return findBuffetForStudent(buffetRequest.alumnoId())
                .map(this::responseFrom);
    }

    private void ensureStudentExists(UUID studentId) {
        if (!alumnoRepository.existsById(studentId)) {
            throw new AlumnoNoEncontradoException(studentId);
        }
    }

    private Optional<Buffet> findBuffetForStudent(UUID studentId) {
        return buffetRepository.findByAlumnoId(studentId);
    }

    private GetBuffetByStudentResponse responseFrom(Buffet buffet) {
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
