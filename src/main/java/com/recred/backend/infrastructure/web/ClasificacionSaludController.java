package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.domain.models.ClasificacionSalud;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clasificaciones-salud")
public class ClasificacionSaludController {

    private final GetHealthRestrictionsCatalog getHealthRestrictionsCatalog;

    public ClasificacionSaludController(GetHealthRestrictionsCatalog getHealthRestrictionsCatalog) {
        this.getHealthRestrictionsCatalog = getHealthRestrictionsCatalog;
    }

    @GetMapping
    public ResponseEntity<List<ClasificacionSalud>> getCatalog() {
        List<ClasificacionSalud> catalog = getHealthRestrictionsCatalog.execute();
        return ResponseEntity.ok(catalog);
    }
}
