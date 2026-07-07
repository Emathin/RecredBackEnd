package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdatePickupSlotRules {

    private final FranjaHorariaRepository timeSlotRepository;

    public UpdatePickupSlotRules(FranjaHorariaRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public UpdatePickupSlotRulesResponse execute(UpdatePickupSlotRulesRequest rulesRequest) {
        FranjaHoraria timeSlot = findTimeSlot(rulesRequest);
        applyPickupRules(timeSlot, rulesRequest);
        return UpdatePickupSlotRulesResponse.fromDomain(timeSlotRepository.save(timeSlot));
    }

    private FranjaHoraria findTimeSlot(UpdatePickupSlotRulesRequest rulesRequest) {
        return timeSlotRepository.findById(rulesRequest.slotId())
                .orElseThrow(() -> new PickupSlotRulesNotFoundException(rulesRequest.slotId()));
    }

    private void applyPickupRules(
            FranjaHoraria timeSlot,
            UpdatePickupSlotRulesRequest rulesRequest
    ) {
        timeSlot.setCupoMaximo(rulesRequest.cupoMaximo());
        timeSlot.setMinutosCorte(cutoffMinutesFrom(rulesRequest.minutosCorte()));
    }

    private int cutoffMinutesFrom(Integer minutosCorte) {
        if (minutosCorte == null) {
            return 0;
        }

        if (minutosCorte < 0) {
            throw new IllegalArgumentException("minutosCorte no puede ser negativo");
        }

        return minutosCorte;
    }
}
