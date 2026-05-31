package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.domain.models.HealthClassification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clasificaciones-salud")
public class HealthClassificationController {

    private final GetHealthRestrictionsCatalog getCatalog;

    public HealthClassificationController(GetHealthRestrictionsCatalog getCatalog) {
        this.getCatalog = getCatalog;
    }

    @GetMapping
    public ResponseEntity<List<HealthClassification>> getCatalog() {
        List<HealthClassification> catalog = getCatalog.ejecutar();
        return ResponseEntity.ok(catalog);
    }
}