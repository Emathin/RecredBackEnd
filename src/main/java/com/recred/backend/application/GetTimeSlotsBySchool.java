package com.recred.backend.application;

import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.util.List;

public class GetTimeSlotsBySchool {

    private final FranjaHorariaRepository repository;

    public GetTimeSlotsBySchool(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    public List<GetTimeSlotsBySchoolResponse> execute(GetTimeSlotsBySchoolRequest request) {
        return repository.findAllByColegioId(request.colegioId()).stream()
                .map(slot -> new GetTimeSlotsBySchoolResponse(
                        slot.getId(),
                        slot.getColegioId(),
                        slot.getDescripcion(),
                        slot.getHoraInicio(),
                        slot.getHoraFin(),
                        slot.isActivo(),
                        slot.getCupoMaximo(),
                        slot.getMinutosCorte()
                ))
                .toList();
    }
}