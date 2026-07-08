package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TimeSlotNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateTimeSlot {

    private final FranjaHorariaRepository repository;

    public DeactivateTimeSlot(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TimeSlotDetailsResponse execute(DeactivateTimeSlotRequest request) {
        TimeSlotValidation.validateIds(request.colegioId(), request.slotId());
        FranjaHoraria slot = repository.findByIdAndColegioId(
                        request.slotId(),
                        request.colegioId())
                .orElseThrow(() -> new TimeSlotNotFoundException(request.slotId()));
        slot.setActivo(false);
        return TimeSlotDetailsResponse.fromDomain(repository.save(slot));
    }
}
