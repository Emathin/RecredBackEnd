package com.recred.backend.application;

import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;

import java.util.Optional;

public class GetBuffetByAlumno {

    private final BuffetRepository buffetRepository;
    private final AlumnoRepository alumnoRepository;

    public GetBuffetByAlumno(BuffetRepository buffetRepository, AlumnoRepository alumnoRepository) {
        this.buffetRepository = buffetRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public Optional<GetBuffetByAlumnoResponse> execute(GetBuffetByAlumnoRequest request) {
        if (!alumnoRepository.existsById(request.alumnoId())) {
            throw new AlumnoNoEncontradoException(request.alumnoId());
        }
        return buffetRepository.findByAlumnoId(request.alumnoId())
                .map(buffet -> new GetBuffetByAlumnoResponse(
                        buffet.getId(),
                        buffet.getNombre(),
                        buffet.getColegio() != null ? buffet.getColegio().getId() : null,
                        buffet.getVendedor() != null ? buffet.getVendedor().getId() : null,
                        buffet.getFechaVencimientoHabilitacion(),
                        buffet.getActivo(),
                        buffet.getCreadoEn(),
                        buffet.getActualizadoEn()
                ));
    }
}
