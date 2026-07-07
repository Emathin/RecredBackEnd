package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.util.List;
import java.util.UUID;

public class GetTimeSlotsBySchool {

    private final FranjaHorariaRepository timeSlotRepository;

    public GetTimeSlotsBySchool(FranjaHorariaRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<GetTimeSlotsBySchoolResponse> execute(
            GetTimeSlotsBySchoolRequest slotsRequest) {
        List<FranjaHoraria> slots = findTimeSlotsBySchool(slotsRequest.colegioId());
        return responsesFrom(slots);
    }

    private List<FranjaHoraria> findTimeSlotsBySchool(UUID schoolId) {
        return timeSlotRepository.findAllByColegioId(schoolId);
    }

    private List<GetTimeSlotsBySchoolResponse> responsesFrom(List<FranjaHoraria> slots) {
        return slots.stream()
                .map(this::responseFrom)
                .toList();
    }

    private GetTimeSlotsBySchoolResponse responseFrom(FranjaHoraria slot) {
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
