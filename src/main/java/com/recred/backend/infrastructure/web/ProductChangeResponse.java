package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.Producto;

import java.util.List;

public record ProductChangeResponse(
    @JsonProperty("productosMenosVendidos")
    List<Producto> leastSoldProducts
){}
