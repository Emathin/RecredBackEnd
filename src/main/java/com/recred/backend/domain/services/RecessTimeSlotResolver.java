package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class RecessTimeSlotResolver {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final FranjaHorariaRepository timeSlotRepository;
    private final BuffetRepository buffetRepository;

    public RecessTimeSlotResolver(FranjaHorariaRepository timeSlotRepository) {
        this(timeSlotRepository, null);
    }

    public RecessTimeSlotResolver(
            FranjaHorariaRepository timeSlotRepository,
            BuffetRepository buffetRepository
    ) {
        this.timeSlotRepository = timeSlotRepository;
        this.buffetRepository = buffetRepository;
    }

    public FranjaHoraria resolve(Alumno student, RecessTime recessTime) {
        if (recessTime == RecessTime.ELEVEN_AM || recessTime == RecessTime.FOUR_PM) {
            return virtualSlotFor(recessTime);
        }
        return resolveBySchoolId(schoolId(student), recessTime);
    }

    public FranjaHoraria resolveByBuffet(UUID buffetId, RecessTime recessTime) {
        if (recessTime == RecessTime.ELEVEN_AM || recessTime == RecessTime.FOUR_PM) {
            return virtualSlotFor(recessTime);
        }
        return resolveBySchoolId(schoolId(buffetId, recessTime), recessTime);
    }

    private FranjaHoraria resolveBySchoolId(UUID schoolId, RecessTime recessTime) {
        List<FranjaHoraria> slots = timeSlotRepository.findAllByColegioId(schoolId)
                .stream()
                .sorted(Comparator.comparing(FranjaHoraria::getHoraInicio))
                .toList();

        return slots.stream()
                .filter(slot -> matches(slot, recessTime))
                .findFirst()
                .orElseGet(() -> slotByPosition(slots, schoolId, recessTime));
    }

    private UUID schoolId(Alumno student) {
        if (student.getGrado() == null
                || student.getGrado().getColegio() == null
                || student.getGrado().getColegio().getId() == null) {
            throw new PickupTimeSlotNotFoundException(null, null);
        }

        return student.getGrado().getColegio().getId();
    }

    private UUID schoolId(UUID buffetId, RecessTime recessTime) {
        if (buffetRepository == null) {
            throw new PickupTimeSlotNotFoundException(null, recessTime);
        }

        return buffetRepository.findById(buffetId)
                .map(Buffet::getColegio)
                .map(Colegio::getId)
                .orElseThrow(() -> new PickupTimeSlotNotFoundException(null, recessTime));
    }

    private FranjaHoraria slotByPosition(
            List<FranjaHoraria> slots,
            UUID schoolId,
            RecessTime recessTime
    ) {
        int index = switch (recessTime) {
            case FIRST_RECESS -> 0;
            case SECOND_RECESS -> 1;
            case NOON -> 2;
            case AFTER_HOURS -> 3;
            case ELEVEN_AM, FOUR_PM -> throw new PickupTimeSlotNotFoundException(
                    schoolId, recessTime);
        };

        if (slots.size() > index) {
            return slots.get(index);
        }

        throw new PickupTimeSlotNotFoundException(schoolId, recessTime);
    }

    private boolean matches(FranjaHoraria slot, RecessTime recessTime) {
        String description = normalize(slot.getDescripcion());
        return switch (recessTime) {
            case FIRST_RECESS -> containsAll(description, "primer", "recreo");
            case SECOND_RECESS -> containsAll(description, "segundo", "recreo");
            case NOON -> containsAny(description, "mediodia", "medio dia", "almuerzo");
            case AFTER_HOURS -> containsAny(description, "salida", "despues", "final");
            case ELEVEN_AM, FOUR_PM -> false;
        };
    }

    private boolean containsAll(String message, String... values) {
        return List.of(values).stream().allMatch(message::contains);
    }

    private boolean containsAny(String message, String... values) {
        return List.of(values).stream().anyMatch(message::contains);
    }

    private String normalize(String value) {
        String lowerCaseValue = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        String normalizedValue = Normalizer.normalize(lowerCaseValue, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalizedValue).replaceAll("");
    }

    private FranjaHoraria virtualSlotFor(RecessTime recessTime) {
        FranjaHoraria virtualSlot = new FranjaHoraria();
        virtualSlot.setId(null);
        if (recessTime == RecessTime.ELEVEN_AM) {
            virtualSlot.setDescripcion("11:00 hs");
            virtualSlot.setHoraInicio(java.time.LocalTime.of(11, 0));
            virtualSlot.setHoraFin(java.time.LocalTime.of(12, 0));
        } else if (recessTime == RecessTime.FOUR_PM) {
            virtualSlot.setDescripcion("16:00 hs");
            virtualSlot.setHoraInicio(java.time.LocalTime.of(16, 0));
            virtualSlot.setHoraFin(java.time.LocalTime.of(17, 0));
        }
        return virtualSlot;
    }
}
