package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.util.List;

public class ListSchoolTimeSlots {

    private final FranjaHorariaRepository repository;

    public ListSchoolTimeSlots(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    public List<TimeSlotDetailsResponse> execute(ListSchoolTimeSlotsRequest request) {
        TimeSlotValidation.validateSchoolId(request.colegioId());
        return slots(request).stream()
                .map(TimeSlotDetailsResponse::fromDomain)
                .toList();
    }

    private List<FranjaHoraria> slots(ListSchoolTimeSlotsRequest request) {
        if (request.includeInactive()) {
            return repository.findAllByColegioIdIncludingInactive(request.colegioId());
        }

        return repository.findAllByColegioId(request.colegioId());
    }
}
