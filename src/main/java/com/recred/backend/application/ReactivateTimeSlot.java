package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TimeSlotNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.springframework.transaction.annotation.Transactional;

public class ReactivateTimeSlot {

    private final FranjaHorariaRepository repository;

    public ReactivateTimeSlot(FranjaHorariaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TimeSlotDetailsResponse execute(ReactivateTimeSlotRequest request) {
        TimeSlotValidation.validateIds(request.colegioId(), request.slotId());
        FranjaHoraria slot = slot(request);
        validate(slot, request);
        slot.setActivo(true);
        return TimeSlotDetailsResponse.fromDomain(saveSlot(slot));
    }

    private FranjaHoraria slot(ReactivateTimeSlotRequest request) {
        return repository.findByIdAndColegioId(request.slotId(), request.colegioId())
                .orElseThrow(() -> new TimeSlotNotFoundException(request.slotId()));
    }

    private void validate(FranjaHoraria slot, ReactivateTimeSlotRequest request) {
        TimeSlotValidation.validateTimeRange(slot.getHoraInicio(), slot.getHoraFin());
        TimeSlotValidation.requireNoActiveOverlap(
                repository,
                request.colegioId(),
                request.slotId(),
                slot.getHoraInicio(),
                slot.getHoraFin());
    }

    private FranjaHoraria saveSlot(FranjaHoraria slot) {
        return repository.save(slot);
    }
}
