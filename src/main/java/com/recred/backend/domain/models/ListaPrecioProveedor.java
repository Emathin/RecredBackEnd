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
public class ListaPrecioProveedor {
    private UUID id;
    private UUID proveedorId;
    private String urlArchivo;
    private String nombreOriginal;
    private boolean activa = true;
    private LocalDateTime creadoEn;
    private List<ItemListaPrecioProveedor> items = new ArrayList<>();
}
