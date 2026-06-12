package com.recred.backend.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaIA {
    private UUID id;
    private UUID alumnoId;
    private UUID productoId;
    private Producto producto;
    private String titulo;
    private String mensaje;
}
