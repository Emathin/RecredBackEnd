package com.recred.backend.config;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DefaultHealthClassificationsInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_CLASSIFICATIONS = List.of(
            "Sin TACC",
            "Sin Sodio",
            "Apto Vegano",
            "Contiene Huevo",
            "Contiene Pescado/Mariscos",
            "Contiene Soja",
            "Apto Vegetariano",
            "Sin Azúcar",
            "Contiene Lácteos",
            "Tiene Maní"
    );

    private final ClasificacionSaludRepository repository;

    public DefaultHealthClassificationsInitializer(ClasificacionSaludRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULT_CLASSIFICATIONS.forEach(this::ensureClassificationExists);
    }

    private void ensureClassificationExists(String descripcion) {
        repository.findByDescripcion(descripcion)
                .ifPresentOrElse(
                        this::activateClassification,
                        () -> saveClassification(descripcion)
                );
    }

    private void activateClassification(ClasificacionSalud clasificacion) {
        if (Boolean.TRUE.equals(clasificacion.getActivo())) {
            return;
        }
        clasificacion.setActivo(true);
        repository.save(clasificacion);
    }

    private void saveClassification(String descripcion) {
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setDescripcion(descripcion);
        clasificacion.setActivo(true);
        repository.save(clasificacion);
    }
}
