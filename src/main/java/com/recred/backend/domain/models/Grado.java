package com.recred.backend.domain.models;

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
    private Nivel nivel;
    private String anio;
    private String division;
    private Boolean activo = true;
}
