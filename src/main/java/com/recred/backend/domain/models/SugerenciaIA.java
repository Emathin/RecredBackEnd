package com.recred.backend.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaIA {
    private UUID id;
    private UUID alumnoId;
    private UUID buffetId;
    private UUID productoId;
    private Producto producto;
    private TipoSugerenciaIA tipo;
    private String titulo;
    private String mensaje;
    private Map<String, Object> metadata;
    private Boolean aceptada;
    private LocalDateTime respondidaEn;
}
