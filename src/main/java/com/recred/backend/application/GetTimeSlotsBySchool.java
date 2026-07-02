package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.util.List;
import java.util.UUID;

public class GetTimeSlotsBySchool {

    private final FranjaHorariaRepository repository;

    public GetTimeSlotsBySchool(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    public List<GetTimeSlotsBySchoolResponse> execute(GetTimeSlotsBySchoolRequest request) {
        List<FranjaHoraria> slots = this.findTimeSlotsByColegioId(request.colegioId());
        return this.mapToResponse(slots);
    }

    private List<FranjaHoraria> findTimeSlotsByColegioId(UUID colegioId) {
        return repository.findAllByColegioId(colegioId);
    }

    private List<GetTimeSlotsBySchoolResponse> mapToResponse(List<FranjaHoraria> slots) {
        return slots.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GetTimeSlotsBySchoolResponse mapToResponse(FranjaHoraria slot) {
        return new GetTimeSlotsBySchoolResponse(
                slot.getId(),
                slot.getColegioId(),
                slot.getDescripcion(),
                slot.getHoraInicio(),
                slot.getHoraFin(),
                slot.isActivo(),
                slot.getCupoMaximo(),
                slot.getMinutosCorte()
        );
    }
}