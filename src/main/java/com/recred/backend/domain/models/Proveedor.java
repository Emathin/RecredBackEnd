package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Proveedor {
    private UUID id;
    private UUID kiosqueroId;
    private String nombre;
    private String telefono;
    private String email;
    private String diasVisita;
    private String notas;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private List<ListaPrecioProveedor> listasPrecios = new ArrayList<>();
}
