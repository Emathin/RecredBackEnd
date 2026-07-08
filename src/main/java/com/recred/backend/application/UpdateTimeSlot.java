package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.exceptions.TimeSlotInUseException;
import com.recred.backend.domain.exceptions.TimeSlotNotFoundException;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class UpdateTimeSlot {

    private static final List<EstadoCompra> ACTIVE_ADVANCE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final FranjaHorariaRepository franjaHorariaRepository;
    private final ColegioRepository colegioRepository;
    private final PurchaseRepository purchaseRepository;
    private final Clock clock;

    public UpdateTimeSlot(
            FranjaHorariaRepository franjaHorariaRepository,
            ColegioRepository colegioRepository,
            PurchaseRepository purchaseRepository,
            Clock clock
    ) {
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.colegioRepository = colegioRepository;
        this.purchaseRepository = purchaseRepository;
        this.clock = clock;
    }

    @Transactional
    public TimeSlotDetailsResponse execute(UpdateTimeSlotRequest request) {
        TimeSlotValidation.validateIds(request.colegioId(), request.slotId());
        String descripcion = TimeSlotValidation.validatedDescription(request.descripcion());
        TimeSlotValidation.validateTimeRange(request.horaInicio(), request.horaFin());
        Integer cupoMaximo = TimeSlotValidation.validatedCapacity(request.cupoMaximo());
        int minutosCorte = TimeSlotValidation.validatedCutoff(request.minutosCorte());
        requireActiveSchool(request);
        FranjaHoraria slot = slot(request);
        requireEditable(slot, request, descripcion);
        requireNoOverlap(slot, request);
        update(slot, request, descripcion, cupoMaximo, minutosCorte);
        return TimeSlotDetailsResponse.fromDomain(saveSlot(slot));
    }

    private void requireActiveSchool(UpdateTimeSlotRequest request) {
        if (!colegioRepository.existsActiveById(request.colegioId())) {
            throw new SchoolNotFoundException(request.colegioId());
        }
    }

    private FranjaHoraria slot(UpdateTimeSlotRequest request) {
        return franjaHorariaRepository.findByIdAndColegioId(
                        request.slotId(),
                        request.colegioId())
                .orElseThrow(() -> new TimeSlotNotFoundException(request.slotId()));
    }

    private void requireEditable(
            FranjaHoraria slot,
            UpdateTimeSlotRequest request,
            String descripcion
    ) {
        if (!TimeSlotValidation.hasStructuralChanges(
                slot,
                descripcion,
                request.horaInicio(),
                request.horaFin())) {
            return;
        }

        if (purchaseRepository.existsActiveFutureScheduledByPickupSlotId(
                slot.getId(),
                LocalDate.now(clock),
                ACTIVE_ADVANCE_STATUSES)) {
            throw new TimeSlotInUseException(slot.getId());
        }
    }

    private void requireNoOverlap(FranjaHoraria slot, UpdateTimeSlotRequest request) {
        if (!slot.isActivo()) {
            return;
        }

        TimeSlotValidation.requireNoActiveOverlap(
                franjaHorariaRepository,
                request.colegioId(),
                request.slotId(),
                request.horaInicio(),
                request.horaFin());
    }

    private void update(
            FranjaHoraria slot,
            UpdateTimeSlotRequest request,
            String descripcion,
            Integer cupoMaximo,
            int minutosCorte
    ) {
        slot.setDescripcion(descripcion);
        slot.setHoraInicio(request.horaInicio());
        slot.setHoraFin(request.horaFin());
        slot.setCupoMaximo(cupoMaximo);
        slot.setMinutosCorte(minutosCorte);
    }

    private FranjaHoraria saveSlot(FranjaHoraria slot) {
        return franjaHorariaRepository.save(slot);
    }
}
