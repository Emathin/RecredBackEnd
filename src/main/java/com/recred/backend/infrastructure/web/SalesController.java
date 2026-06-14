package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetProductStatistics;
import com.recred.backend.domain.models.ProductSaleStatistics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kiosqueros/{idUsuario}/productos/{idProducto}/estadisticas")
public class SalesController {

    private final GetProductStatistics getProductStatistics;

    public SalesController(GetProductStatistics getProductStatistics) {
        this.getProductStatistics = getProductStatistics;
    }

    @GetMapping
    public ResponseEntity<ProductSaleStatistics> getProductStatistics(
            @PathVariable("idUsuario") UUID idKiosquero,
            @PathVariable("idProducto") UUID idProducto
    ) {
        return ResponseEntity.ok(getProductStatistics.execute(idProducto, idKiosquero));
    }
}
