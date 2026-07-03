package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TimeSlotNotFoundException;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

public class GetTimeSlot {

    private final FranjaHorariaRepository repository;

    public GetTimeSlot(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    public TimeSlotDetailsResponse execute(GetTimeSlotRequest request) {
        TimeSlotValidation.validateIds(request.colegioId(), request.slotId());
        return repository.findByIdAndColegioId(request.slotId(), request.colegioId())
                .map(TimeSlotDetailsResponse::fromDomain)
                .orElseThrow(() -> new TimeSlotNotFoundException(request.slotId()));
    }
}
