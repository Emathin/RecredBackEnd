package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdatePickupSlotRules {

    private final FranjaHorariaRepository franjaHorariaRepository;

    public UpdatePickupSlotRules(FranjaHorariaRepository franjaHorariaRepository) {
        this.franjaHorariaRepository = franjaHorariaRepository;
    }

    @Transactional
    public UpdatePickupSlotRulesResponse execute(UpdatePickupSlotRulesRequest request) {
        FranjaHoraria slot = franjaHorariaRepository.findById(request.slotId())
                .orElseThrow(() -> new PickupSlotRulesNotFoundException(request.slotId()));

        slot.setCupoMaximo(request.cupoMaximo());
        slot.setMinutosCorte(validatedCutoff(request.minutosCorte()));
        return UpdatePickupSlotRulesResponse.fromDomain(franjaHorariaRepository.save(slot));
    }

    private int validatedCutoff(Integer minutosCorte) {
        if (minutosCorte == null) {
            return 0;
        }

        if (minutosCorte < 0) {
            throw new IllegalArgumentException("minutosCorte no puede ser negativo");
        }

        return minutosCorte;
    }
}
