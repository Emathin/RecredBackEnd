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
        FranjaHoraria slot = findSlot(request);
        slot.setActivo(false);
        return TimeSlotDetailsResponse.fromDomain(saveSlot(slot));
    }

    private FranjaHoraria findSlot(DeactivateTimeSlotRequest request) {
        return repository.findByIdAndColegioId(request.slotId(), request.colegioId())
                .orElseThrow(() -> new TimeSlotNotFoundException(request.slotId()));
    }

    private FranjaHoraria saveSlot(FranjaHoraria slot) {
        return repository.save(slot);
    }
}
