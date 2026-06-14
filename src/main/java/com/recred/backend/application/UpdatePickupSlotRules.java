package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UpdatePickupSlotRules {

    private final FranjaHorariaRepository franjaHorariaRepository;

    public UpdatePickupSlotRules(FranjaHorariaRepository franjaHorariaRepository) {
        this.franjaHorariaRepository = franjaHorariaRepository;
    }

    @Transactional
    public FranjaHoraria execute(UUID slotId, Integer cupoMaximo, Integer minutosCorte) {
        FranjaHoraria slot = franjaHorariaRepository.findById(slotId)
                .orElseThrow(() -> new PickupSlotRulesNotFoundException(slotId));

        slot.setCupoMaximo(cupoMaximo);
        slot.setMinutosCorte(validatedCutoff(minutosCorte));
        return franjaHorariaRepository.save(slot);
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
