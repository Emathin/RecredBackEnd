package com.recred.backend.dominio.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Grado {

    private UUID id;
    private Colegio colegio;
    private String anio;
    private String division;
    private Boolean activo = true;
}