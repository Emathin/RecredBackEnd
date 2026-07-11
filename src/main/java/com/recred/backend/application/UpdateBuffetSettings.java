package com.recred.backend.application;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.BuffetRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBuffetSettings {

    private final BuffetRepository buffetRepository;

    public UpdateBuffetSettings(BuffetRepository buffetRepository) {
        this.buffetRepository = buffetRepository;
    }

    public UpdateBuffetSettingsResponse execute(UpdateBuffetSettingsRequest request) {
        Buffet buffet = buffetRepository.findById(request.buffetId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Buffet no encontrado con ID: " + request.buffetId()));

        if (request.habilitarVentasAnticipadasNoLaborables() != null) {
            buffet.setHabilitarVentasAnticipadasNoLaborables(
                    request.habilitarVentasAnticipadasNoLaborables());
        }

        buffetRepository.save(buffet);

        return new UpdateBuffetSettingsResponse(
                buffet.getId(),
                buffet.getHabilitarVentasAnticipadasNoLaborables()
        );
    }
}
