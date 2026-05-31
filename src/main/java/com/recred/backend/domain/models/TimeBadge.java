package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TimeBadge {

    private UUID id;
    private UUID schoolId;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active = true;

    public TimeBadge(UUID id, UUID schoolId, String description,
                     LocalTime horaInicio, LocalTime endTime, boolean active) {
        this.id = id;
        this.schoolId = schoolId;
        this.description = description;
        this.startTime = horaInicio;
        this.endTime = endTime;
        this.active = active;
    }

    public boolean isWithinTheBadge(LocalDateTime moment) {
        if (moment == null) {
            return false;
        }

        LocalTime time = moment.toLocalTime();

        return !time.isBefore(this.startTime) && !time.isAfter(this.endTime);
    }
}