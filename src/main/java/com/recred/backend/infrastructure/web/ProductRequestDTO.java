package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SaveProductCommand;
import com.recred.backend.application.UpdateProductCommand;
import com.recred.backend.domain.models.TipoManejoInventario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductRequestDTO(
        @NotBlank String nombre,
        @NotBlank String descripcion,
        @NotNull @Positive BigDecimal precio,
        BigDecimal peso,
        @NotNull Boolean requierePreparacion,
        UUID categoriaId,
        String nuevaCategoriaNombre,
        UUID buffetId,
        Integer stockActual,
        TipoManejoInventario tipoManejoInventario,
        Integer cupoMaximoDiario,
        Set<UUID> clasificacionesSaludIds,
        Set<UUID> tiposIds,
        String urlImagen
) {

    public SaveProductCommand toSaveCommand() {
        return new SaveProductCommand(
                nombre,
                descripcion,
                precio,
                peso,
                requierePreparacion,
                categoriaId,
                nuevaCategoriaNombre,
                buffetId,
                stockActual,
                clasificacionesSaludIds,
                tiposIds,
                tipoManejoInventario,
                cupoMaximoDiario,
                urlImagen
        );
    }

    public UpdateProductCommand toUpdateCommand(UUID id) {
        return new UpdateProductCommand(
                id,
                nombre,
                descripcion,
                precio,
                peso,
                requierePreparacion,
                categoriaId,
                nuevaCategoriaNombre,
                buffetId,
                stockActual,
                clasificacionesSaludIds,
                tiposIds,
                tipoManejoInventario,
                cupoMaximoDiario,
                urlImagen
        );
    }
}
