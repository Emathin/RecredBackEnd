package com.recred.backend.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventario {
    private UUID id;
    private UUID buffetId;
    private UUID productoId;
    private Integer stockActual;
    private Integer stockMinimo;
    private EstadoInventario estado;
    private LocalDateTime actualizadoEn;
}
