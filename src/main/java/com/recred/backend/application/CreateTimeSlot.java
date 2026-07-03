package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreateTimeSlot {

    private final FranjaHorariaRepository franjaHorariaRepository;
    private final ColegioRepository colegioRepository;

    public CreateTimeSlot(
            FranjaHorariaRepository franjaHorariaRepository,
            ColegioRepository colegioRepository
    ) {
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.colegioRepository = colegioRepository;
    }

    @Transactional
    public TimeSlotDetailsResponse execute(CreateTimeSlotRequest request) {
        TimeSlotValidation.validateSchoolId(request.colegioId());
        String descripcion = TimeSlotValidation.validatedDescription(request.descripcion());
        TimeSlotValidation.validateTimeRange(request.horaInicio(), request.horaFin());
        Integer cupoMaximo = TimeSlotValidation.validatedCapacity(request.cupoMaximo());
        int minutosCorte = TimeSlotValidation.validatedCutoff(request.minutosCorte());
        requireActiveSchool(request);
        TimeSlotValidation.requireNoActiveOverlap(
                franjaHorariaRepository,
                request.colegioId(),
                null,
                request.horaInicio(),
                request.horaFin());

        return TimeSlotDetailsResponse.fromDomain(franjaHorariaRepository.save(
                slot(request, descripcion, cupoMaximo, minutosCorte)));
    }

    private void requireActiveSchool(CreateTimeSlotRequest request) {
        if (!colegioRepository.existsActiveById(request.colegioId())) {
            throw new SchoolNotFoundException(request.colegioId());
        }
    }

    private FranjaHoraria slot(
            CreateTimeSlotRequest request,
            String descripcion,
            Integer cupoMaximo,
            int minutosCorte
    ) {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setColegioId(request.colegioId());
        slot.setDescripcion(descripcion);
        slot.setHoraInicio(request.horaInicio());
        slot.setHoraFin(request.horaFin());
        slot.setActivo(true);
        slot.setCupoMaximo(cupoMaximo);
        slot.setMinutosCorte(minutosCorte);
        return slot;
    }
}
