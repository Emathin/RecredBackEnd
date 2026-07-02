package com.recred.backend.application;

import com.recred.backend.domain.exceptions.BuffetNotFoundException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.Buffet;

import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.util.List;
import java.util.UUID;

public class GetBuffetTimeSlots {

    private final BuffetRepository buffetRepository;
    private final FranjaHorariaRepository franjaHorariaRepository;

    public GetBuffetTimeSlots(
            BuffetRepository buffetRepository,
            FranjaHorariaRepository franjaHorariaRepository
    ) {
        this.buffetRepository = buffetRepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
    }

    public List<GetTimeSlotsBySchoolResponse> execute(UUID buffetId) {
        Buffet buffet = buffetRepository.findById(buffetId)
                .orElseThrow(() -> new BuffetNotFoundException(buffetId));

        if (buffet.getColegio() == null || buffet.getColegio().getId() == null) {
            throw new PickupTimeSlotNotFoundException(null, null);
        }

        return franjaHorariaRepository.findAllByColegioId(buffet.getColegio().getId())
                .stream()
                .map(slot -> new GetTimeSlotsBySchoolResponse(
                        slot.getId(),
                        slot.getColegioId(),
                        slot.getDescripcion(),
                        slot.getHoraInicio(),
                        slot.getHoraFin(),
                        slot.isActivo(),
                        slot.getCupoMaximo(),
                        slot.getMinutosCorte()
                ))
                .toList();
    }
}
