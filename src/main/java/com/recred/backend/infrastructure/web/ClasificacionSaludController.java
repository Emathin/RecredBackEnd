package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetHealthRestrictionsCatalogRequest;
import com.recred.backend.application.GetHealthRestrictionsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clasificaciones-salud")
public class ClasificacionSaludController {

    private final GetHealthRestrictionsCatalog obtenerCatalogo;

    public ClasificacionSaludController(GetHealthRestrictionsCatalog obtenerCatalogo) {
        this.obtenerCatalogo = obtenerCatalogo;
    }

    @GetMapping
    public ResponseEntity<List<GetHealthRestrictionsResponse>> obtenerCatalogo() {
        return ResponseEntity.ok(obtenerCatalogo
                .execute(new GetHealthRestrictionsCatalogRequest())
                .restrictions());
    }
}
