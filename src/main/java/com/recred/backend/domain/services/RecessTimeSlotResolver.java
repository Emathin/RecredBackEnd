package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RecessTime;
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

    public RecessTimeSlotResolver(FranjaHorariaRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public FranjaHoraria resolve(Alumno student, RecessTime recessTime) {
        UUID schoolId = schoolId(student);
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
}
