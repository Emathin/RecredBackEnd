package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Grade {

    private UUID id;
    private School school;
    private String anio;
    private String division;
    private Boolean activo = true;
}