package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.Product;

import java.util.List;

public record ProductChangeResponse(
    @JsonProperty("productosMenosVendidos")
    List<Product> leastSoldProducts
){}
