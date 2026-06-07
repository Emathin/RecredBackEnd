package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.Summary;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class SummaryResponse {
    private String id;
    private String fechaDesde;
    private String fechaHasta;
    private String resumen;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

    public SummaryResponse(Summary summary) {
        this.id = summary.getId().toString();
        this.fechaDesde = summary.getFechaDesde().format(FORMATTER);
        this.fechaHasta = summary.getFechaHasta().format(FORMATTER);
        this.resumen = summary.getResumen();
    }
}
