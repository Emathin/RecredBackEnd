package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalTime;

public record TimeSlotRequest(
        @JsonProperty("descripcion")
        @JsonAlias("description")
        @NotBlank(message = "La descripcion es requerida")
        String descripcion,

        @JsonProperty("horaInicio")
        @JsonAlias({"startTime", "hora_inicio"})
        @NotNull(message = "horaInicio es requerida")
        LocalTime horaInicio,

        @JsonProperty("horaFin")
        @JsonAlias({"endTime", "hora_fin"})
        @NotNull(message = "horaFin es requerida")
        LocalTime horaFin,

        @JsonProperty("cupoMaximo")
        @JsonAlias({"capacity", "cupo_maximo"})
        @PositiveOrZero(message = "cupoMaximo no puede ser negativo")
        Integer cupoMaximo,

        @JsonProperty("minutosCorte")
        @JsonAlias({"cutoffMinutes", "minutos_corte"})
        @PositiveOrZero(message = "minutosCorte no puede ser negativo")
        Integer minutosCorte
) {
}
